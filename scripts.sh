sudo yum update -y 
sudo yum install -y mariadb-server 
sudo systemctl start mariadb 
sudo systemctl enable mariadb 

sudo yum install java-17-amazon-corretto-devel
# sudo yum remove java-17-amazon-corretto-devel

# export VER="9.0.62"
# wget https://archive.apache.org/dist/tomcat/tomcat-9/v${VER}/bin/apache-tomcat-${VER}.tar.gz
# tar -xf apache-tomcat-${VER}.tar.gz

# cd apache-tomcat-${VER}/bin
# ./catalina.sh start

# cd

# maven
sudo wget https://repos.fedorapeople.org/repos/dchen/apache-maven/epel-apache-maven.repo -O /etc/yum.repos.d/epel-apache-maven.repo

sudo sed -i s/\$releasever/6/g /etc/yum.repos.d/epel-apache-maven.repo
sudo yum install -y apache-maven

sudo /usr/sbin/alternatives --config java
sudo /usr/sbin/alternatives --config javac

sudo amazon-linux-extras install nginx1
sudo systemctl start nginx
sudo systemctl status nginx

# scp -i ~/.ssh/linux2 webapp-0.0.1-SNAPSHOT.jar ec2-user@100.26.210.248:
# scp -i ~/.ssh/linux2 /Users/tengfeiwang/Desktop/Tim/DevOp/CSYE6225/Assignments/a4/assignment4_packer/webapp/myapp.service ec2-user@100.26.210.248:
# java -jar webapp-0.0.1-SNAPSHOT.jar

sudo cp myapp.service /etc/systemd/system/

sudo systemctl enable myapp.service
sudo systemctl start myapp.service