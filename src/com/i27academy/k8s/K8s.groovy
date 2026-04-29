package com.i27academy.k8s

class K8s implements Serializable {

    def jenkins

    K8s(jenkins) {
        this.jenkins = jenkins
    }

    def authlogin(clusterName, clusterZone, projectId) {
        jenkins.sh """
            gcloud container clusters get-credentials ${clusterName} --zone ${clusterZone} --project ${projectId}
            kubectl get nodes
        """
    }


// create a method to deploy k8s applications
def k8sdeploy(filename,docker_image,namespace) {
    jenkins.sh """
        echo "deploying into gke cluster"
        sed -i "s|DIT|${docker_image}|g" .cicd/${filename}
        kubectl apply -f .cicd/k8s_dev.yaml -n ${namespace}
    """
}
}