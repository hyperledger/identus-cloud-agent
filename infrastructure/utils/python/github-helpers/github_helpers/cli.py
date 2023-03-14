"""
GitHub helpers for CI/CD scripts

Following actions are available:
* Getting latest version of a package from GitHub packages
* Downloading arts from GitHub Actions pipelines
"""

import dataclasses
import logging
import sys

# pylint: disable=import-error
import click

# pylint: disable=E0402
from .api import GithubApi


@dataclasses.dataclass
class Errors:
    """Error codes for GitHub helpers"""

    UNABLE_TO_DOWNLOAD = 1
    EMPTY_VERSIONS_LIST = 2


def init_logger():
    """Initializes logger

    :return: Initialized logger
    :rtype: logging.Logger
    """
    logger = logging.getLogger("GitHub Helpers")
    logger.setLevel(logging.DEBUG)
    chandler = logging.StreamHandler()
    chandler.setLevel(logging.DEBUG)
    formatter = logging.Formatter(
        "%(asctime)s - %(name)s - %(levelname)s - %(message)s"
    )
    chandler.setFormatter(formatter)
    logger.addHandler(chandler)
    return logger


# pylint: disable=R0903
class Globals:
    """Global variables to share between entry points"""

    def __init__(self, token, owner, repo):
        """Default constructor

        :param token: GitHub API token
        :type token: str
        :param owner: GitHub owner(user)
        :type owner: str
        :param repo: GitHub repository name
        :type repo: str
        """
        self.api = GithubApi(token=token, owner=owner, repo=repo)
        self.logger = init_logger()


pass_globals = click.make_pass_decorator(Globals)


@click.group()
@click.option(
    "--token",
    envvar="GITHUB_TOKEN",
    metavar="TOKEN",
    required=True,
    help="GitHub authentication token.",
)
@click.option(
    "--owner",
    default="input-output-hk",
    metavar="OWNER",
    help="GitHub owner(user).",
)
@click.option(
    "--repo",
    default="atala-prism-android-app",
    metavar="REPOSITORY",
    help="GitHub repo(project).",
)
@click.pass_context
def cli(ctx, token, owner, repo):
    """Command line interface entry point
    \f

    :param ctx: Shared context
    :type ctx: click.core.Context
    :param token: GitHub API token
    :type token: str
    :param owner: GitHub owner(user)
    :type owner: str
    :param repo: GitHub repository name
    :type repo: str
    """
    ctx.obj = Globals(token, owner, repo)


@cli.command()
@click.option(
    "--package",
    default="io.iohk.atala.prism-identity",
    metavar="PACKAGE",
    help="Package name.",
)
@click.option(
    "--package-type",
    default="maven",
    metavar="TYPE",
    help="Package type.",
)
@pass_globals
def get_latest_package_version(ctx, package, package_type):
    """Gets latest version of package
    \f

    :param ctx: Shared context
    :type ctx: github_helpers.click_main.Globals
    :param package: Name of GitHub package
    :type package: str
    :param package_type: Package type
    :type package_type: str
    """
    versions = ctx.api.get_package_versions(package, package_type=package_type)
    if not versions:
        ctx.logger.error(
            f"Specified package {package} doesn't exist." "Versions list is empty."
        )
        sys.exit(Errors.EMPTY_VERSIONS_LIST)
    else:
        try:
            if package_type == 'container':
                print(versions[0].get("metadata").get("container").get("tags")[0])
            else:
                print(versions[0].get("name", "NOT EXIST"))
        except IndexError:
            print("NOT EXIST")


@cli.command()
@pass_globals
def get_latest_release_version(ctx):
    """Gets latest version of the release
    \f

    :param ctx: Shared context
    :type ctx: github_helpers.click_main.Globals
    """
    versions = ctx.api.get_release_versions()
    if not versions:
        ctx.logger.error("Releases list is empty.")
        sys.exit(Errors.EMPTY_VERSIONS_LIST)
    else:
        print(versions[0].get("tag_name", "NOT EXIST"))


@cli.command()
@click.option(
    "--run-id",
    metavar="RUN_ID",
    default="latest",
    help="ID of GitHub Actions run.",
)
@click.option(
    "--dest-dir",
    metavar="DEST",
    default=".",
    help="Download destination directory.",
)
@click.option(
    "--branch",
    metavar="BRANCH",
    default="master",
    help="GitHub branch to search for latest artifacts.",
)
@pass_globals
def download_arts(ctx, run_id, dest_dir, branch):
    """Downloads artifacts from GitHub
    \f

    :param ctx: Shared context
    :type ctx: github_helpers.click_main.Globals
    :param run_id: GitHub Actions run ID
    :type run_id: str
    :param dest_dir: Download destination directory
    :type dest_dir: str
    :param branch: Branch for latest arts download
    :type branch: str
    """
    ctx.logger.info(
        "Downloading artifacts for " f"run_id='{run_id}' and branch='{branch}'"
    )
    wf_artifacts = {}
    if run_id in ("latest", ""):
        wf_runs = ctx.api.get_workflow_runs(branch)
        for run in wf_runs["workflow_runs"]:
            wf_artifacts = ctx.api.get_workflow_artifacts(run["id"])
            if wf_artifacts.get("total_count", 0) > 0:
                break
    else:
        wf_artifacts = ctx.api.get_workflow_artifacts(run_id)
    if wf_artifacts.get("total_count", 0) > 0:
        for art in wf_artifacts["artifacts"]:
            artifact_id = art.get("id")
            artifact_name = art.get("name")
            ctx.api.download_artifact(
                artifact_id, filename=f"{artifact_name}.zip", destdir=dest_dir
            )
    else:
        ctx.logger.error(
            "No runs with artifacts found for "
            f"run_id='{run_id}' and branch='{branch}'"
        )
        sys.exit(Errors.UNABLE_TO_DOWNLOAD)
