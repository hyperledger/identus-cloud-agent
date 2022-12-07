FROM jupyter/scipy-notebook

# Install Python 3 packages
RUN pip install 'qrcode' 'pymongo' 'didcomm' 'peerdid==0.4.0'

# Downgrade packages
# See authlib==1.0.0b1 https://github.com/sicpa-dlab/didcomm-python/blob/ca962db30ac7c1492ed6e3654575cce2dd3a5e0e/didcomm/core/from_prior.py#L107
RUN pip install 'authlib==1.2.0'
