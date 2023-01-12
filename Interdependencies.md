# Interdependencies

This document describes the interdependencies between the different Building Blocks.



```mermaid
flowchart
  PLUTO-didcomm -.WIP.-> OR1{OR} 
  didcommx --external dependency--> OR1{OR} 

  subgraph PRISM
    prism-cryto --> prism-node-1.4
    prism-cryto --> OR2{OR}
    prism-node-1.4 -.copy proto files.-> prism-node

    subgraph BB[Building Blocks Interdependencies]
      prism-node -.???\nMAYBE in the future.-> castor
      prism-node -.???\nMAYBE in the future.-> pollux

      pluto -.WIP.-> OR2{OR}
      pluto --> PLUTO-didcomm

      OR1 --> mercury

      OR2 --> castor

      subgraph REPO [BB Repository]
        shared --> castor
        shared --> pollux
        shared --> prism-agent
        shared --> connect
        
        mercury --> pollux
        mercury --> connect
        mercury --> prism-agent
        mercury --> mediator

        connect --> prism-agent

        castor --> iris
        castor --> prism-agent


        infrastructure
        test
      end
    end
  end

  

%% All 
castor[Castor]
connect
infrastructure
iris[IRIS]
mercury[MERCURY]
mediator((Mercury\nMediator))
pluto[PLUTO extrenal repo]
pollux[POLLUX]
prism-agent((Prism Agent))
prism-node[prism-node-client]
shared
test
```

**Note:**
The **Mercury Libraries** is the collection of dependencies with the single cross version.
We will represent as a single box here. But the real dependencies can of a subset of the Mercury Libraries.
For more information about the Mercury Modules Interdependencies see [HERE](./mercury/mercury-library/README.md).