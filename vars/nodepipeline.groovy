// this is node pipine not java ok remeber sligly diffenrt these pipeline
// this is user page 
//use shared libray so use import statement to library
import com.i27academy.k8s.K8s


def call (Map pipelineParams) {

    // instance of k8s class
    def k8s = new K8s(this) // this is jenkins instance that we pass to class constructor

   


pipeline {
    agent {
        label 'java-slave'
    }


    environment {
        APPLICATION_NAME = "${pipelineParams.appName}"
        // DOCKER_HUB = "docker.io/dockerhubraghu"
        // DOCKER_CREDENTIALS = credentials('raghu_dockerhub_creds')

        //JFROG DETAILS
        JFROG_DOCKER_REGISTRY ="trial8oqwds.jfrog.io"
        JFROG_DOCKER_REPO_NAME = "private1-docker"
        JFROG_CREDS = credentials("JFROG_CREDS") //AVAIBLE IN MANAGE JENKINS


        // kuberenetes details for dev cluster
        DEV_CLUSTER_NAME = "cart-cluster"            
        DEV_CLUSTER_ZONE = "us-central1-a"  
        DEV_CLUSTER_PROJECT_ID= "project-026d6e39-3aa1-477a-82a"

        // file name for deployments
        K8S_DEV_FILE = "k8s_dev.yaml"
        K8S_TEST_FILE = "k8s_test.yaml"
        K8S_STAGE_FILE = "k8s_stage.yaml"
        K8S_PROD_FILE = "k8s_prod.yaml"

        // default namespaces
        DEV_NAMESPACE = "cart-dev-ns" 
        TEST_NAMESPACE = "cart-test-ns"
        STAGE_NAMESPACE= "cart-stage-ns" 
        PROD_NAMESPACE = "cart-prod-ns"
    }

   // parametes: used to tale imnput
   
    parameters {
        choice(name: 'docker_build_and_push', 
             choices: ['yes', 'no'], description: 'Build and push Docker image')
        choice(name: 'deploy_to_dev', 
             choices: ['yes', 'no'], description: 'Deploy to dev environment')  
        choice(name: 'deploy_to_test', 
             choices: ['no', 'yes'], description: 'Deploy to test environment') 
        choice(name: 'deploy_to_stage', 
             choices: ['no', 'yes'], description: 'Deploy to stage environment')
        choice(name: 'deploy_to_prod',
             choices: ['no', 'yes'], description: 'Deploy to prod environment')


    }

    stages {

        stage('Docker Build and Push') {
                when {
                    anyOf { //if only push do ok
    
                        expression { params.docker_build_and_push == 'yes' }
                    }
                }       
            steps {
                script {
                    dockerBuildandPush().call()
                }
            }
        }

            stage('Deploy to dev env') {
            when {
                anyOf {
                    expression { params.deploy_to_dev == 'yes' }
                }
            }
            steps {
                script {
                    def docker_image = "${env.JFROG_DOCKER_REGISTRY}/${env.DOCKER_REPO_NAME}/${env.APPLICATION_NAME}:$GIT_COMMIT"
                    k8s.authlogin("${env.DEV_CLUSTER_NAME}", "${env.DEV_CLUSTER_ZONE}", "${env.DEV_CLUSTER_PROJECT_ID}")
                    imagevalidation().call()
                    k8s.k8sdeploy("${env.K8S_DEV_FILE}", docker_image, "${env.DEV_NAMESPACE}")
                }
            }
        }
        
        
        stage('Deploy to test env') { 
            when {
                anyOf {
                        expression { params.deploy_to_test == 'yes' }
                }
            }
            steps {
               script {
                    buildapp().call()
                    imagevalidation().call()
                    dockerDeploy('test',6232).call()
                }
            }
        }

       stage('Deploy to stage env') {
            when {
                anyOf {
                        expression { params.deploy_to_stage == 'yes' }
                }

                anyOf {
                    branch 'release/*'
                      tag pattern: "v\\d{1,2}\\.\\d{1,2}\\.\\d{1,2}", comparator: "REGEXP"
                }
            }
            steps {
                script {
                    //image validation
                    buildapp().call()  
                    imagevalidation().call() 
                    dockerDeploy('stage',7232).call()
                 }
            }
        }

        stage('Deploy to prod') { 
            // approvals neede?
            //brnach and tag condition
            //we also wrote sometime timeout with in 5 min approve and deploy other wise stop.
            when {
                anyOf {
                        expression { params.deploy_to_prod == 'yes' }
                }
                anyOf {
                      tag pattern: "v\\d{1,2}\\.\\d{1,2}\\.\\d{1,2}", comparator: "REGEXP"
                }
            }
            steps { //omly few people push by input mechanism implemented
                timeout(time: 1800, unit: 'SECONDS') {
                       input message: 'Deploy to production?', ok: 'Deploy', submitter: 'raghu, madhu'
                }
                script {
                     
                      dockerDeploy('prod',8232).call()
                 } 
            }
        }
        
    }
}   
}

// ------------------------***-these actual methods we write here to call in above stages to implement the every stage in pipeline simply and multiple tiems clal these methods******* ---------------------------------------------------------------------------

def buildapp(){
    return {
        echo "*** Building ${env.APPLICATION_NAME} application"
        sh "mvn clean package -DskipTests"
    }
}

    
// we can also simply write docker build and push just mention metond and call
def dockerBuildandPush() {
    return {
        echo "*** Building Docker image and pushing to registry"

        // ✅ Fix: Use '.' as the build context (full repo root)
        // -f flag explicitly points to the Dockerfile inside .cicd/
        sh "docker build --no-cache -t ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:$GIT_COMMIT -f ./.cicd/Dockerfile ."
        
        echo "*** Logging into docker registry ***"
          sh "docker login ${env.JFROG_DOCKER_REGISTRY} -u ${JFROG_CREDS_USR} -p ${JFROG_CREDS_PSW}"
        
        echo "*** Pushing docker image to registry ***"
          sh "docker push ${env.JFROG_DOCKER_REGISTRY}/${env.JFROG_DOCKER_REPO_NAME }/${env.APPLICATION_NAME}:$GIT_COMMIT"
    }
}





// define method
//envDeploy,port is a variable
def dockerDeploy(envDeploy,port) {
    return {
        echo "*** Deploying Docker image to  environment"
                // fisrt you connect the dev serever using these withcredentials :, then stop the container if exist, remove the container if exist, then create and run the container
                //  how to secure docker credentilas using withcreds block
                withCredentials([usernamePassword(credentialsId: 'dev_madhu_creds', passwordVariable: 'PASSWORD', usernameVariable: 'USERNAME')]) {
                  
                   script {
                    try {
                    // stop the container
                    // vmipaddress is pvt ip placed in manage jenkins syyste environment varbles name and pvt ip
                   sh "sshpass -p $PASSWORD -v ssh -o StrictHostKeyChecking=no $USERNAME@$docker_vm_ip_address \"docker stop ${env.APPLICATION_NAME}-${envDeploy}\""
                   // remove the container
                   sh "sshpass -p $PASSWORD -v ssh -o StrictHostKeyChecking=no $USERNAME@$docker_vm_ip_address \"docker rm ${env.APPLICATION_NAME}-${envDeploy}\""
                    } 
                    catch (error) {
                        echo "Container ${env.APPLICATION_NAME}-${envDeploy} does not exist, skipping stop and remove steps."
                    }
                    // craete and run the container
                   sh "sshpass -p $PASSWORD -v ssh -o StrictHostKeyChecking=no $USERNAME@$docker_vm_ip_address \"docker run --name ${env.APPLICATION_NAME}-${envDeploy} -p $port:8232 -d ${env.DOCKER_HUB}/${env.APPLICATION_NAME}:$GIT_COMMIT\""
                   }

                }
    }
}

def imagevalidation() {
    return {
        println ("*** Validating Docker image in registry ***")
        try {
            sh "docker pull ${env.JFROG_DOCKER_REGISTRY}/${env.DOCKER_REPO_NAME}/${env.APPLICATION_NAME}:$GIT_COMMIT"
            echo "*** Docker image validation successful ***"
        } catch (error) { 
            println ( "*****docker image not availble in registry, so we create and push the image to registry***")
          //  buildapp().call()  no neede n=because docker builad and push no build needed
            dockerBuildandPush().call() // THEN CALL THE DOCKER BUILD and push method  ok
        }

    }
}



















// yeppudaina sytntax wrong unte munde fail avutundo





















// pipeline {
//     environment {
//         Application _Name = 'eureka'
//        POM_VERSION = readMavenPom().getVersion() //read pom and fetch the version that stores in one vatrible
//        POM_PACKAGING =readMavenPom().getPackaging() //read pom and fetch the packaging that stores in one vatrible
//     }
//     stages {
//         stage('FormatBuild') {
//             // existsing i27-eureka-0.0.1-SNAPSHOT.jar
//             // Destination: i27-eureka-buildnumber-brachname.jar
//             steps {
//                 echo " testing existing jar is i27-${env.APPLICATION_NAME}-${env.POM_VERSION}.${env.POM_PACKAGING}"
//                 echo "testing jar destination is i27-${env.APPLICATION_NAME}-${env.BUILD_NUMBER}-${env.BRANCH_NAME}.${env.POM_PACKAGING}"
              
//             }
//         }
//     }
// }