workspace {

    !identifiers hierarchical

    model {

        !include atalaPrism.dsl
    }

    views {
        systemContext atalaPrism "SystemContext" {
            include *
            autoLayout
        }

        container atalaPrism "BBContainers" "Building Block Containers" {
            include *
            exclude element.tag==Database
            exclude "element.tag==Message Queue"
            exclude "element.tag==Existing Container"
            exclude "element.tag==Existing System"
            autoLayout
        }

        container atalaPrism "Castor" "Castor Container" {
            include ->atalaPrism.castorGroup atalaPrism.castorGroup->
            autoLayout
        }

        container atalaPrism "Iris" "Iris Container" {
            include ->atalaPrism.dltGroup atalaPrism.dltGroup->
            autoLayout
        }

        component atalaPrism.mobileApp "Mob" "Mobile App Components" {
            include *
            autoLayout
        }

        component atalaPrism.castorApi "CastorAPI" "Castor API Components" {
            include *
            autoLayout
        }

        component atalaPrism.castorWorker "CastorWorker" "Castor Worker Component" {
            include *
            autoLayout
        }

        theme default

        styles {
            element "Existing System" {
                background #999999
                color #ffffff
            }
            element "Existing Container" {
                background #999999
                color #ffffff
            }
            element "Database" {
                shape Cylinder
            }
            element "Message Queue" {
                shape Pipe
            }
            element "Mobile App" {
                shape MobileDeviceLandscape
            }
        }
    }

}