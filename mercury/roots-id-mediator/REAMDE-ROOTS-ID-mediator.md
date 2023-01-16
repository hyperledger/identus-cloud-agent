# ROOTS-ID MEDIATOR

See [ATL-1857](https://input-output.atlassian.net/browse/ATL-1857) from more info.

## Note LICENSE

This [ROOTS-ID's Mediator probject is LICENSE](https://raw.githubusercontent.com/roots-id/didcomm-mediator/main/LICENSE) under the [apache-2.0](https://www.apache.org/licenses/LICENSE-2.0)

## Goal

The idea is to test the interoperability of our agents (the client side) against this mediator.

So our client agents should work exactly the same way with this mediator and our mediator.

This will serve as a sanity check that we are compliant with DID Comm v2.

## Starting point

### Git submodules

We added a link to the ROOTS-ID Mediator's repository as a git submodule.

**NOTE:** The submodule is tracking the HEAD of the master branch.

```shell
git submodule init
git submodule update

# or use the flag '--recurse-submodules' when cloning our repository
# git clone --recurse-submodules ...
```

### BUILD working station

- Build the `docker` image for Jupyter.

```shell
docker build -f ./Dockerfile . --platform=linux/amd64 -t atala/didcomm-jupyter
```

```shell
docker run -p 8888:8888 -v /home/fabio/workspace/iohk/atala-prism-building-blocks/mercury/roots-id-mediator/didcomm-mediator/sample-notebooks:/home/jovyan atala/didcomm-jupyter
```

### Build the mediator (docker image)

- Build the `docker` image for the ROOTS-ID mediator (`rodopincha/didcomm-mediator`).

```shell
cd ./didcomm-mediator/
docker build -f ./Dockerfile . --platform=linux/amd64 -t rodopincha/didcomm-mediator
```

### Start the mediator and working station

- Start `docker-compose` (mongodb container instance + mediator container instance)

```shell
docker-compose up # -d
```

- Find the out-of-band invitation link.

  This inforamation is print on the logs of the container instance.

  We can get the _oob message in:

  - `http://0.0.0.0:8000/oob_qrcode` - QRcode
  - `http://127.0.0.1:8000/oob_url` - URL

In the logs we can also see something like this:

```shell
> docker logs roots-id-mediator-rootsid-mediator-1

roots-id-mediator-rootsid-mediator-1  | did:peer:2.Ez6LSmLmWmTvwjgLSuUaEQHdHSFWPwyibgzomWjFmnC6FhLnU.Vz6MktNgLh4N1u9KNhDiqe8KZ8bsLzLcqsifoNiUtBoSs9jxf.SeyJpZCI6Im5ldy1pZCIsInQiOiJkbSIsInMiOiJodHRwOi8vMTI3LjAuMC4xOjgwMDAiLCJhIjpbImRpZGNvbW0vdjIiXX0
...
roots-id-mediator-rootsid-mediator-1  | http://127.0.0.1:8000?_oob=eyJ0eXBlIjoiaHR0cHM6Ly9kaWRjb21tLm9yZy9vdXQtb2YtYmFuZC8yLjAvaW52aXRhdGlvbiIsImlkIjoiNDIxZGJiYzgtNTdjYS00MzQxLWFhM2EtZjViNDIxNWM1NjhmIiwiZnJvbSI6ImRpZDpwZWVyOjIuRXo2TFNtTG1XbVR2d2pnTFN1VWFFUUhkSFNGV1B3eWliZ3pvbVdqRm1uQzZGaExuVS5WejZNa3ROZ0xoNE4xdTlLTmhEaXFlOEtaOGJzTHpMY3FzaWZvTmlVdEJvU3M5anhmLlNleUpwWkNJNkltNWxkeTFwWkNJc0luUWlPaUprYlNJc0luTWlPaUpvZEhSd09pOHZNVEkzTGpBdU1DNHhPamd3TURBaUxDSmhJanBiSW1ScFpHTnZiVzB2ZGpJaVhYMCIsImJvZHkiOnsiZ29hbF9jb2RlIjoicmVxdWVzdC1tZWRpYXRlIiwiZ29hbCI6IlJlcXVlc3RNZWRpYXRlIiwiYWNjZXB0IjpbImRpZGNvbW0vdjIiLCJkaWRjb21tL2FpcDI7ZW52PXJmYzU4NyJdfX0
```

- To stop and remove the container instances use `docker-compose down`
