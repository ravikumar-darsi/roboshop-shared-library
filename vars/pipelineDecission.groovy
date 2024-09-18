#!groovy

def decidePipeline(Map configMap){ // here we are receving configMap from the Jenkins file in catalogue
  application = configMap.get("application")
    switch(application) {
        case 'nodejsVM':
            nodejsVM(configMap)
            break
        case 'javaVM':
            javaVM(configMap)
            break
        case 'nodejsEKS':
            nodejsEKS(configMap)
            break
        default:
            error "Application is not recognised"
            break
  }
}