def call (String dockerRegistry, String dockerImageTag, String helmChartName, String awsCredID, String awsRegion, String eksClusterName) {
    sh """
        if ! command -v helm > /dev/null; then
            echo "Helm not found. Installing Helm..."
            curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/main/scripts/get-helm-3
            chmod 700 get_helm.sh
            ./get_helm.sh
            rm -f get_helm.sh
            echo "Helm installed successfully."
        fi
    """

    sh """
        if ! command -v aws > /dev/null; then
            echo "AWS CLI not found. Installing AWS CLI..."
            curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip" 2> /dev/null
            
            dpkg-query -l unzip 1>/dev/null || true
            if [ \$? -ne 0 ]; then
                echo "Unzip is not installed. Installing unzip..."
                sudo apt update &> /dev/null
                sudo apt -y install unzip &> /dev/null
                echo "Unzip installed successfully."
            fi
            
            unzip awscliv2.zip > /dev/null
            sudo ./aws/install > /dev/null
            rm -rf awscliv2.zip aws
            echo "AWS CLI installed successfully."
        fi
        if ! command -v kubectl > /dev/null; then
            curl -LO "https://dl.k8s.io/release/\$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
            sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl
        fi
    """

    withCredentials([usernamePassword(
        credentialsId: "$awsCredID",
        usernameVariable: "awsAccessKey",
        passwordVariable: "awsSecretKey"
    )]) {
            sh """
                aws configure set aws_access_key_id $awsAccessKey
                aws configure set aws_secret_access_key $awsSecretKey
                aws configure set region $awsRegion

                aws eks --region ap-south-1 update-kubeconfig --name eks-cluster
            """
        }
    sh 'helm upgrade --install $helmChartName helm/ --set image.repository="$dockerRegistry:$dockerImageTag" '
}
    
}
