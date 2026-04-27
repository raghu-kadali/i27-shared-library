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

}