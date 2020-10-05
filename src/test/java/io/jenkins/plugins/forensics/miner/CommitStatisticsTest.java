package io.jenkins.plugins.forensics.miner;

import java.util.ArrayList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.hm.hafner.util.FilteredLog;
import edu.hm.hafner.util.SerializableTest;

import static io.jenkins.plugins.forensics.assertions.Assertions.*;

/**
 * Tests the class {@link CommitStatistics}.
 *
 * @author Ullrich Hafner
 */
class CommitStatisticsTest extends SerializableTest<CommitStatistics> {
    private static final String AUTHOR = "author";

    @Override
    protected CommitStatistics createSerializable() {
        List<Commit> commits = new ArrayList<>();

        Commit first = new Commit("1", AUTHOR, 0);
        first.addLines(3).deleteLines(2);
        commits.add(first);
        Commit second = new Commit("2", "anotherAuthor", 2);
        second.addLines(3).deleteLines(4);
        commits.add(second);

        return new CommitStatistics(commits);
    }

    @Test
    @SuppressWarnings("checkstyle:JavaNCSS")
    void shouldCountCorrectly() {
        List<Commit> commits = new ArrayList<>();

        assertThat(CommitStatistics.countChanges(commits)).isZero();
        assertThat(CommitStatistics.countDeletes(commits)).isZero();
        assertThat(CommitStatistics.countMoves(commits)).isZero();

        CommitStatistics empty = new CommitStatistics(commits);
        assertThat(empty).hasAddedLines(0)
                .hasDeletedLines(0)
                .hasLinesOfCode(0)
                .hasAbsoluteChurn(0)
                .hasAuthorCount(0)
                .hasCommitCount(0);

        Assertions.assertThat(logCommits(commits).getInfoMessages()).containsExactly(
                "-> 0 commits analyzed",
                "-> 0 lines added",
                "-> 0 lines added");

        Commit first = new Commit("1", AUTHOR, 0);
        first.addLines(3).deleteLines(2);
        commits.add(first);

        Assertions.assertThat(CommitStatistics.countChanges(commits)).isOne();
        Assertions.assertThat(CommitStatistics.countDeletes(commits)).isZero();
        Assertions.assertThat(CommitStatistics.countMoves(commits)).isZero();
        CommitStatistics firstCommit = new CommitStatistics(commits);
        assertThat(firstCommit).hasAddedLines(3)
                .hasDeletedLines(2)
                .hasLinesOfCode(1)
                .hasAbsoluteChurn(5)
                .hasAuthorCount(1)
                .hasCommitCount(1);

        Assertions.assertThat(logCommits(commits).getInfoMessages()).containsExactly(
                "-> 1 commits analyzed",
                "-> 1 MODIFY commits",
                "-> 3 lines added",
                "-> 2 lines added");

        Commit second = new Commit("2", "anotherAuthor", 2);
        second.addLines(3).deleteLines(4);
        commits.add(second);

        Assertions.assertThat(CommitStatistics.countChanges(commits)).isEqualTo(2);
        Assertions.assertThat(CommitStatistics.countDeletes(commits)).isZero();
        Assertions.assertThat(CommitStatistics.countMoves(commits)).isZero();
        CommitStatistics secondCommit = new CommitStatistics(commits);
        assertThat(secondCommit).hasAddedLines(6)
                .hasDeletedLines(6)
                .hasLinesOfCode(0)
                .hasAbsoluteChurn(12)
                .hasAuthorCount(2)
                .hasCommitCount(2);

        Assertions.assertThat(logCommits(commits).getInfoMessages()).containsExactly(
                "-> 2 commits analyzed",
                "-> 2 MODIFY commits",
                "-> 6 lines added",
                "-> 6 lines added");

        Commit third = new Commit("2", AUTHOR, 2);
        third.setNewPath(Commit.NO_FILE_NAME);
        third.setOldPath("old");
        commits.add(third);

        Assertions.assertThat(CommitStatistics.countChanges(commits)).isEqualTo(2);
        Assertions.assertThat(CommitStatistics.countDeletes(commits)).isEqualTo(1);
        Assertions.assertThat(CommitStatistics.countMoves(commits)).isZero();
        CommitStatistics thirdCommit = new CommitStatistics(commits);
        assertThat(thirdCommit).hasAddedLines(6)
                .hasDeletedLines(6)
                .hasLinesOfCode(0)
                .hasAbsoluteChurn(12)
                .hasAuthorCount(2)
                .hasCommitCount(2);

        Assertions.assertThat(logCommits(commits).getInfoMessages()).containsExactly(
                "-> 2 commits analyzed",
                "-> 2 MODIFY commits",
                "-> 1 DELETE commits",
                "-> 6 lines added",
                "-> 6 lines added");

        Commit forth = new Commit("3", AUTHOR, 3);
        forth.setNewPath("new");
        forth.setOldPath("old");
        commits.add(forth);

        Assertions.assertThat(CommitStatistics.countChanges(commits)).isEqualTo(2);
        Assertions.assertThat(CommitStatistics.countDeletes(commits)).isEqualTo(1);
        Assertions.assertThat(CommitStatistics.countMoves(commits)).isEqualTo(1);
        CommitStatistics forthCommit = new CommitStatistics(commits);
        assertThat(forthCommit).hasAddedLines(6)
                .hasDeletedLines(6)
                .hasLinesOfCode(0)
                .hasAbsoluteChurn(12)
                .hasAuthorCount(2)
                .hasCommitCount(3);

        Assertions.assertThat(logCommits(commits).getInfoMessages()).containsExactly(
                "-> 3 commits analyzed",
                "-> 2 MODIFY commits",
                "-> 1 RENAME commits",
                "-> 1 DELETE commits",
                "-> 6 lines added",
                "-> 6 lines added");
    }

    private FilteredLog logCommits(final List<Commit> commits) {
        FilteredLog log = new FilteredLog("Error");
        CommitStatistics.logCommits(commits, log);
        return log;
    }
}