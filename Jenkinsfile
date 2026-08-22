pipeline {
    agent { label 'java17' }

    environment {
        REPO_URL = "http://${env.MAVEN_URL}/repository/maven-snapshots/"
        REPO_ID = "snapshots"
        PROJECT_NAME = "${env.JOB_BASE_NAME}"
        DIR_API = "${env.DIR_KEY}-api"
        DIR_SERVICE = "${env.DIR_KEY}-service"
        IMAGE_NAME = "${env.REGISTRY_URL}/${PROJECT_NAME}:${env.BUILD_NUMBER}"
    }

    parameters {
        choice(name: 'DEPLOY_API', choices: [false, true], description: '是否发布API')
        choice(name: 'DOCKER_NO_CACHE', choices: [false, true], description: '构建镜像时是否使用 --no-cache')
    }

    tools {
        jdk   'temurin-17'
        maven 'M3'
    }

    stages {
        stage('构建并发布') {
            when { expression { params.DEPLOY_API == "true" } }
            steps {
                script {
                    dir(DIR_API) {
                        sh '''
                            echo '============================== 构建并发布 =============================='
                            mvn clean deploy -U -DskipTests -DaltDeploymentRepository=${REPO_ID}::default::${REPO_URL}
                        '''
                    }
                }
            }
        }
        stage('构建镜像') {
            steps {
                script {
                    def noCacheArg = params.DOCKER_NO_CACHE == "true" ? "--no-cache" : ""
                    sh """
                        echo '============================== 构建镜像 =============================='
                        cp /var/jenkins_home/.m2/settings.xml ./${DIR_SERVICE}/settings.xml
                        cp /var/jenkins_home/apache-maven-3.6.3-bin.tar.gz ./${DIR_SERVICE}/
                        docker build --network openiov-management ${noCacheArg} -t ${IMAGE_NAME} -f /var/jenkins_home/ServiceDockerfile ./${DIR_SERVICE}/
                    """
                }
            }
        }
        stage('上传镜像') {
            steps {
                withCredentials([
                    usernamePassword(
                        credentialsId: 'tencent-tcr',
                        usernameVariable: 'TCR_USERNAME',
                        passwordVariable: 'TCR_PASSWORD'
                    )
                ]) {
                    sh '''
                        echo '============================== 登录腾讯云 TCR =============================='
                        echo "${TCR_PASSWORD}" | docker login \
                            --username "${TCR_USERNAME}" \
                            --password-stdin \
                            ccr.ccs.tencentyun.com

                        echo '============================== 上传镜像 =============================='
                        docker push "${IMAGE_NAME}"

                        docker logout ccr.ccs.tencentyun.com
                    '''
                }
            }
        }
        stage('运行镜像') {
            steps {
                sh '''
                    echo '============================== 运行镜像 =============================='
                    # 1. 清理旧容器 (使用简单的 || true 避免容器不存在时报错)
                    docker stop ${PROJECT_NAME} || true
                    docker rm ${PROJECT_NAME} || true

                    # 2. 运行新容器
                    docker pull ${IMAGE_NAME}
                    docker run -d --name ${PROJECT_NAME} --network appnet ${IMAGE_NAME}

                    echo "开始监控启动日志..."

                    # 使用 count 计数器，配合 while 循环（不依赖 Bash 特性）
                    count=1
                    max_retries=15
                    SUCCESS=0

                    while [ $count -le $max_retries ]
                    do
                        echo "检查服务状态... ($count/$max_retries)"

                        # 检查日志中是否包含成功关键字
                        # 使用 -i 忽略大小写
                        if docker logs ${PROJECT_NAME} 2>&1 | grep -iq "Started .* in .* seconds"; then
                            echo "------------------------------------------------"
                            echo "检测到启动成功标识！"
                            echo "------------------------------------------------"
                            SUCCESS=1
                            break
                        fi

                        # 顺便检查容器是否意外挂掉
                        if [ -z "$(docker ps -q -f name=^/${PROJECT_NAME}$)" ]; then
                            echo "错误：容器已退出，启动失败！"
                            docker logs ${PROJECT_NAME} --tail 50
                            exit 1
                        fi

                        count=$((count + 1))
                        sleep 5
                    done

                    if [ $SUCCESS -eq 0 ]; then
                        echo "错误：服务在 75 秒内未启动成功。"
                        echo "最后 50 行日志如下："
                        docker logs ${PROJECT_NAME} --tail 50
                        exit 1
                    fi
                '''
            }
        }
    }
}