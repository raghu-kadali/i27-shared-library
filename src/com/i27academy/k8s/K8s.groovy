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
def k8sdeploy() {
    jenkins.sh """
        echo "Current dir: \$(pwd)"
        ls -la
        ls -la .cicd/k8s/

        echo "deploying into gke cluster"
        kubectl apply -f ./.cicd/k8s_dev.yaml -n cart-dev-ns
    """
}
}