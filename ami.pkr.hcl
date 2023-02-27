variable "aws_region" {
  type    = string
  default = "us-east-1"
}

variable "source_ami" {
  type    = string
  default = "ami-0dfcb1ef8550277af" # Amazon Linux 2 x64
}

variable "ssh_username" {
  type    = string
  default = "ec2-user"
}

variable "subnet_id" {
  type    = string
  default = "subnet-0032dae14d2616b3a"
}

// variable "profile" {
//   type    = string
//   default = "dev"
// }

variable "aws_demouser" {
  type    = string
  default = "523466750771"
}

variable "aws_devuser" {
  type    = string
  default = "266883092057"
}

// variable "aws_access_key_id" {
//   type    = string
//   default = env("AWS_ACCESS_KEY_ID")
// }

// variable "aws_secret_access_key" {
//   type    = string
//   default = env("AWS_SECRET_ACCESS_KEY")
// }

# https://www.packer.io/plugins/builders/amazon/ebs
source "amazon-ebs" "my-ami" {
  region          = "${var.aws_region}"
  // profile         = "${var.profile}"
  ami_name        = "csye6225_${formatdate("YYYY_MM_DD_hh_mm_ss", timestamp())}"
  ami_description = "AMI for CSYE 6225"
  ami_regions = [
    "us-east-1"
  ]
  ami_users = [
    "${var.aws_devuser}",
    "${var.aws_demouser}",
  ]

  aws_polling {
    delay_seconds = 120
    max_attempts  = 50
  }

  instance_type = "t2.micro"
  source_ami    = "${var.source_ami}"
  ssh_username  = "${var.ssh_username}"
  // vpc_id= "vpc-07f293fb1908fc8b4"
  subnet_id     = "${var.subnet_id}"
  // access_key              = "${var.aws_access_key_id}"
  // secret_key              = "${var.aws_secret_access_key}"
  // security_group_id = "sg-00d6a8e71ffc8ef3c"
  launch_block_device_mappings {
    delete_on_termination = true
    device_name           = "/dev/xvda"
    volume_size           = 50
    volume_type           = "gp2"
  }
}

build {
  sources = ["source.amazon-ebs.my-ami"]

  provisioner "file" {
    sources = ["target/webapp-0.0.1-SNAPSHOT.jar"]
    destination = "~/webapp-0.0.1-SNAPSHOT.jar"
  }
    provisioner "file" {
    sources = ["myapp.service"]
    destination = "~/myapp.service"
  }

  provisioner "shell" {
    environment_vars = [
      "DEBIAN_FRONTEND=noninteractive",
      "CHECKPOINT_DISABLE=1"
    ]
    scripts = [
      "scripts.sh"
    ]
  }
}
