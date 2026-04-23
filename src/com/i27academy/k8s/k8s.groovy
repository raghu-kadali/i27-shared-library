

package com.i27academy.k8s;

class K8s {
    def jenkins
    
    K8s(jenkins) {
        this.jenkins = jenkins
    }
}

//write the method to connect the jekins slave to gke clutsre then call later beascue this is sh library for kubernetes related methods

def authlogin(ClusterName, Zone, ProjectID) {
    jenkins.sh """
    echo "Authenticating to GKE cluster..."
    gcloud container clusters get-credentials ${ClusterName} --zone ${Zone} --project ${ProjectID}
    kubectl get nodes
    """
}






















































// src/com/i27-academy/
// ├── builds/
// │   └── builds.groovy       ← Docker/Build methods only for ci 
// └── k8s/
//     └── k8s.groovy          ← Kubernetes methods used for cd

// vars/
// └── dockerpipeline.groovy   ← Main pipeline (entry point)



