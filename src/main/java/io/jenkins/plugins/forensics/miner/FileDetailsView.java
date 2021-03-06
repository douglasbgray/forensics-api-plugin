package io.jenkins.plugins.forensics.miner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import edu.hm.hafner.echarts.JacksonFacade;
import edu.hm.hafner.echarts.LineSeries;
import edu.hm.hafner.echarts.LineSeries.FilledMode;
import edu.hm.hafner.echarts.LineSeries.StackedMode;
import edu.hm.hafner.echarts.LinesChartModel;
import edu.hm.hafner.echarts.LinesDataSet;
import edu.hm.hafner.echarts.Palette;

import org.kohsuke.stapler.bind.JavaScriptMethod;
import hudson.model.ModelObject;

import io.jenkins.plugins.datatables.DefaultAsyncTableContentProvider;
import io.jenkins.plugins.datatables.TableColumn;
import io.jenkins.plugins.datatables.TableModel;
import io.jenkins.plugins.echarts.AsyncTrendChart;

/**
 * Creates a view for the selected link in the details table.
 *
 * @author Giulia Del Bravo
 */
public class FileDetailsView extends DefaultAsyncTableContentProvider implements ModelObject, AsyncTrendChart {

    private final String fileHash;
    private static final String FILE_NAME_PROPERTY = "fileName.";
    private final RepositoryStatistics repositoryStatistics;
    private final FileStatistics fileStatistics;

    /**
     * Creates a new {@link FileDetailsView} instance.
     *
     * @param fileLink
     *         the file the view should be created for
     * @param repositoryStatistics
     *         the RepositoryStatistic containing the file
     */
    public FileDetailsView(final String fileLink, final RepositoryStatistics repositoryStatistics) {
        super();

        this.fileHash = fileLink.substring(FILE_NAME_PROPERTY.length());
        this.repositoryStatistics = repositoryStatistics;
        fileStatistics = filterStatistics();
    }

    private FileStatistics filterStatistics() {
        return repositoryStatistics.getFileStatistics()
                .stream()
                .filter(f -> String.valueOf(f.getFileName().hashCode()).equals(fileHash)).findAny().orElseThrow(
                        () -> new NoSuchElementException("No file found with hash code " + fileHash));
    }

    /**
     * Should return a LinesChartModel for this file detailing the added and deleted lines over all commits analyzed.
     *
     * @return LinesChartModel for this file displaying deleted and added lines.
     */
    public LinesChartModel createChartModel() {
        return new FileChurnTrendChart().create(fileStatistics);
    }

    @JavaScriptMethod
    @SuppressWarnings("unused") // Called by jelly view
    @Override
    public String getBuildTrendModel() {
        return new JacksonFacade().toJson(createChartModel());
    }

    @Override
    public boolean isTrendVisible() {
        return true;
    }

    @Override
    public String getDisplayName() {
        return Messages.FileView_Title(fileStatistics.getFileName());
    }

    @Override
    public TableModel getTableModel(final String s) {
        return new FileTableModel();
    }

    private class FileTableModel extends TableModel {

        @Override
        public String getId() {
            return "forensics-details";
        }

        @Override
        public List<TableColumn> getColumns() {
            List<TableColumn> columns = new ArrayList<>();

            columns.add(new TableColumn(Messages.Table_Column_CommitId(), "commitId"));
            columns.add(new TableColumn(Messages.Table_Column_Author(), "author"));
            columns.add(new TableColumn(Messages.Table_Column_AddedLines(), "addedLines"));
            columns.add(new TableColumn(Messages.Table_Column_DeletedLines(), "deletedLines"));

            return columns;
        }

        @Override
        public List<Object> getRows() {
            return fileStatistics
                    .getCommits()
                    .stream()
                    .map(commitId -> new ForensicsRow(fileStatistics, commitId))
                    .collect(Collectors.toList());
        }
    }

    /**
     * A table row that shows details about a specific file.
     */
    @SuppressWarnings("PMD.DataClass") // Used to automatically convert to JSON object
    public static class ForensicsRow {
        private final FileStatistics fileStatistics;
        private final String commitId;

        ForensicsRow(final FileStatistics fileStatistics, final String commitId) {

            this.fileStatistics = fileStatistics;
            this.commitId = commitId;
        }

        public int getAddedLines() {
            return fileStatistics.getAddedLines(commitId);
        }

        public int getDeletedLines() {
            return fileStatistics.getDeletedLines(commitId);
        }

        public String getCommitId() {
            return commitId;
        }

        public String getAuthor() {
            return fileStatistics.getAuthor(commitId);
        }
    }

    static class FileChurnTrendChart {
        private static final String ADDED_KEY = "added";
        private static final String DELETED_KEY = "deleted";

        public LinesChartModel create(final FileStatistics fileStatistics) {
            LinesDataSet dataSet = createDataSetPerCommit(fileStatistics);

            LinesChartModel model = new LinesChartModel();
            Palette[] colors = {Palette.GREEN, Palette.RED};
            model.setDomainAxisLabels(dataSet.getDomainAxisLabels());
            model.setBuildNumbers(dataSet.getBuildNumbers());
            int index = 0;
            for (String name : dataSet.getDataSetIds()) {
                LineSeries series = new LineSeries(name, colors[index++].getNormal(), StackedMode.SEPARATE_LINES,
                        FilledMode.LINES);
                series.addAll(dataSet.getSeries(name));
                model.addSeries(series);
            }
            return model;
        }

        private LinesDataSet createDataSetPerCommit(final FileStatistics current) {
            LinesDataSet model = new LinesDataSet();
            for (String commitId : current.getCommits()) {
                model.add(commitId, computeSeries(current, commitId));
            }
            return model;
        }

        private Map<String, Integer> computeSeries(final FileStatistics fileStatistics, final String commitId) {
            Map<String, Integer> commitChanges = new HashMap<>();
            commitChanges.put(ADDED_KEY, fileStatistics.getAddedLinesOfCommit().get(commitId));
            commitChanges.put(DELETED_KEY, fileStatistics.getDeletedLinesOfCommit().get(commitId));
            return commitChanges;
        }
    }

}
