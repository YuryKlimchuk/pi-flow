package com.hydroyura.agents.piflow.workspace.infrastructure;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.hydroyura.agents.piflow.workspace.domain.LocalGit;
import com.hydroyura.agents.piflow.workspace.domain.WorkspaceGitException;
import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.springframework.stereotype.Component;

/**
 * Адаптер локального Git на базе JGit.
 */
@Component
public class JGitWorkspaceAdapter implements LocalGit {

	@Override
	public void cloneRepository(String repositoryUrl, Path directory, String targetBranch) {
		try {
			Git.cloneRepository()
					.setURI(repositoryUrl)
					.setDirectory(directory.toFile())
					.setBranch(targetBranch)
					.setCloneAllBranches(true)
					.call()
					.close();
		} catch (GitAPIException e) {
			throw WorkspaceGitException.repositoryAccess("failed to clone repository: " + repositoryUrl, e);
		}
	}

	@Override
	public void updateToRemote(Path directory, String targetBranch) {
		try (Git git = Git.open(directory.toFile())) {
			git.fetch().call();
			checkoutOrCreate(git, targetBranch, "refs/remotes/origin/" + targetBranch);
			git.reset()
					.setMode(ResetCommand.ResetType.HARD)
					.setRef("refs/remotes/origin/" + targetBranch)
					.call();
		} catch (GitAPIException | IOException e) {
			throw WorkspaceGitException.git("failed to update repository: " + directory, e);
		}
	}

	@Override
	public String resolveBaseBranch(String repositoryUrl, String preferredBranch) {
		try {
			Collection<Ref> refs = Git.lsRemoteRepository()
					.setHeads(true)
					.setRemote(repositoryUrl)
					.call();
			Set<String> heads = refs.stream()
					.map(ref -> ref.getName().replaceFirst("^refs/heads/", ""))
					.collect(Collectors.toSet());
			if (heads.contains(preferredBranch)) {
				return preferredBranch;
			}
			if (heads.contains("main")) {
				return "main";
			}
		} catch (GitAPIException ignored) {
			// не удалось перечислить удалённые ветки — оставляем предпочтительную,
			// нижестоящие операции выдадут внятную ошибку при реальной несовместимости.
		}
		return preferredBranch;
	}

	@Override
	public boolean isRepository(Path directory) {
		Path gitDir = directory.resolve(".git");
		if (!Files.exists(gitDir)) {
			return false;
		}
		try (Git git = Git.open(directory.toFile())) {
			return git.getRepository().getObjectDatabase().exists();
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean isClean(Path directory) {
		try (Git git = Git.open(directory.toFile())) {
			Status status = git.status().call();
			if (!status.getAdded().isEmpty()
					|| !status.getChanged().isEmpty()
					|| !status.getConflicting().isEmpty()
					|| !status.getMissing().isEmpty()
					|| !status.getModified().isEmpty()
					|| !status.getRemoved().isEmpty()) {
				return false;
			}
			for (String untracked : status.getUntracked()) {
				if (!untracked.startsWith(".pi-flow/")) {
					return false;
				}
			}
			return true;
		} catch (GitAPIException | IOException e) {
			throw WorkspaceGitException.git("failed to read repository status: " + directory, e);
		}
	}

	@Override
	public String currentBranch(Path directory) {
		try (Git git = Git.open(directory.toFile())) {
			return git.getRepository().getBranch();
		} catch (IOException e) {
			throw WorkspaceGitException.git("failed to read current branch: " + directory, e);
		}
	}

	@Override
	public boolean branchExists(Path directory, String branch) {
		try (Git git = Git.open(directory.toFile())) {
			return git.getRepository().findRef("refs/heads/" + branch) != null;
		} catch (IOException e) {
			throw WorkspaceGitException.git("failed to read branches: " + directory, e);
		}
	}

	@Override
	public void createBranch(Path directory, String branch, String fromBranch) {
		try (Git git = Git.open(directory.toFile())) {
			git.branchCreate().setName(branch).setStartPoint(fromBranch).call();
		} catch (GitAPIException | IOException e) {
			throw WorkspaceGitException.git("failed to create branch " + branch, e);
		}
	}

	@Override
	public void checkout(Path directory, String branch) {
		try (Git git = Git.open(directory.toFile())) {
			git.checkout().setName(branch).call();
		} catch (GitAPIException | IOException e) {
			throw WorkspaceGitException.git("failed to checkout branch " + branch, e);
		}
	}

	@Override
	public String commit(Path directory, String message, List<String> relativePaths) {
		try (Git git = Git.open(directory.toFile())) {
			AddCommand add = git.add();
			if (relativePaths.isEmpty()) {
				add.addFilepattern(".");
			} else {
				relativePaths.forEach(add::addFilepattern);
			}
			add.call();

			Status status = git.status().call();
			if (status.getAdded().isEmpty() && status.getChanged().isEmpty() && status.getRemoved().isEmpty()) {
				return headId(git);
			}

			RevCommit commit = git.commit().setMessage(message).call();
			return commit != null ? commit.name() : headId(git);
		} catch (GitAPIException | IOException e) {
			throw WorkspaceGitException.git("failed to commit changes: " + directory, e);
		}
	}

	private void checkoutOrCreate(Git git, String branch, String startPoint) throws GitAPIException, IOException {
		if (git.getRepository().findRef("refs/heads/" + branch) != null) {
			git.checkout().setName(branch).call();
		} else {
			git.checkout().setName(branch).setCreateBranch(true).setStartPoint(startPoint).call();
		}
	}

	private String headId(Git git) throws IOException {
		ObjectId head = git.getRepository().resolve(Constants.HEAD);
		return head != null ? head.name() : null;
	}
}
