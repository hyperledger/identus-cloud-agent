"""
GitHub Helpers API module
"""
import os
import zipfile

import requests
from requests.adapters import HTTPAdapter
from requests.auth import HTTPBasicAuth
from urllib3 import Retry


class GithubError(Exception):
    """Error happened in GitHub API call"""


class GithubApi:
    """Client for GitHub API"""

    def __init__(self, token=None, owner=None, repo=None, url="https://api.github.com"):
        """Initialize a client to interact with GitHub API.

        :param token: GitHub API access token. Defaults to GITHUB_TOKEN env var
        :param url: The URL of the GitHub API instance
        """
        if not token:
            raise GithubError("Missing or empty GitHub API access token")
        self.token = token
        if not owner or not repo:
            raise GithubError("`owner` and `repo` must be defined for this request.")
        self.owner = owner
        self.repo = repo
        self.url = url
        self._request_session()

    def __repr__(self):
        opts = {
            "token": self.token,
            "url": self.url,
        }
        kwargs = [f"{k}={v!r}" for k, v in opts.items()]
        return f'GithubApi({", ".join(kwargs)})'

    def get_workflow_artifacts(self, run_id):
        """Get workflow artifacts.

        Endpoint:
            GET: ``/repos/{owner}/{repo}/actions/runs/{run_id}/artifacts``
        """
        endpoint = f"repos/{self.owner}/{self.repo}/actions/runs/{run_id}/artifacts"
        resp = self._get(endpoint)
        return resp

    def download_artifact(self, artifact_id, destdir=None, filename=None, unzip=True):
        """Downloads artifact by its ID"""
        endpoint = f"repos/{self.owner}/{self.repo}/actions/artifacts/{artifact_id}/zip"
        path = self._download(endpoint, destdir=destdir, filename=filename)
        if unzip:
            with zipfile.ZipFile(path, "r") as zip_ref:
                zip_ref.extractall(os.path.dirname(path))
                os.remove(path)
        return path

    def get_package_versions(self, package_name, package_type="maven"):
        """Get versions of Maven package at GitHib packages

        Endpoint:
            GET: ``/orgs/{org}/packages/{package_type}/{package_name}/versions``
        """
        endpoint = f"orgs/{self.owner}/packages/{package_type}/{package_name}/versions"
        resp = self._get(endpoint)
        return resp

    def get_release_versions(self):
        """Get versions of Maven package at GitHib packages

        Endpoint:
            GET: ``/orgs/{owner}/{repo}/releases``
        """
        endpoint = f"repos/{self.owner}/{self.repo}/releases"
        resp = self._get(endpoint)
        return resp

    def get_workflow_runs(self, branch=None):
        """Get workflow runs

        Endpoint:
            GET ``/repos/{owner}/{repo}/actions/runs``
        """
        endpoint = f"repos/{self.owner}/{self.repo}/actions/runs"
        if branch:
            endpoint = f"{endpoint}?branch={branch}"
        resp = self._get(endpoint)
        return resp

    def _request_session(
        self,
        retries=3,
        backoff_factor=0.3,
        status_forcelist=(408, 429, 500, 502, 503, 504, 520, 521, 522, 523, 524),
    ):
        """Get a session with Retry enabled.

        :param retries: Number of retries to allow.
        :param backoff_factor: Backoff factor to apply between attempts.
        :param status_forcelist: HTTP status codes to force a retry on.
        """
        self._session = requests.Session()
        retry = Retry(
            total=retries,
            backoff_factor=backoff_factor,
            status_forcelist=status_forcelist,
            allowed_methods=False,
            raise_on_redirect=False,
            raise_on_status=False,
            respect_retry_after_header=False,
        )
        adapter = HTTPAdapter(max_retries=retry)
        self._session.mount("http://", adapter)
        self._session.mount("https://", adapter)
        return self._session

    def _get(self, endpoint):
        """Send a GET HTTP request.

        :param endpoint: API endpoint to call.
        :type endpoint: str

        :raises requests.exceptions.HTTPError: When response code is not successful.
        :returns: A JSON object with the response from the API.
        """
        headers = {"Accept": "application/vnd.github.v3+json"}
        auth = HTTPBasicAuth(self.token, "")
        resp = None
        request_url = "{0}/{1}".format(self.url, endpoint)
        resp = self._session.get(request_url, auth=auth, headers=headers)
        resp.raise_for_status()
        return resp.json()

    def _download(self, endpoint, destdir=None, filename=None):
        """Downloads a file.

        :param endpoint: Endpoint to download from.
        :param destdir: Optional destination directory.
        :param filename: Optional file name. Defaults to download.zip.
        """

        if not filename:
            filename = "download.zip"
        if not destdir:
            destdir = os.getcwd()
        auth = HTTPBasicAuth(self.token, "")
        request_url = "{0}/{1}".format(self.url, endpoint)
        resp = self._session.get(request_url, stream=True, auth=auth)
        path = "{0}/{1}".format(destdir, filename)
        with open(path, "wb") as download_file:
            for chunk in resp.iter_content(chunk_size=1024):
                if chunk:
                    download_file.write(chunk)
        return path
