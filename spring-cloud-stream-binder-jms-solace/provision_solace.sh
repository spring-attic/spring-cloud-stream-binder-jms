#! /bin/bash

set -e

# Input: OVA file location
if [ ! -f "$1" ]; then
  echo 'Please provide a path to the Solace OVA'
  exit 1
fi
ova_path="$1"

# Generate random name for new VM
vm_name=solace-test-$(uuidgen | tr 'A-Z' 'a-z' | cut -d- -f1)

# Import OVA
VBoxManage import ${ova_path} --vsys 0 --vmname ${vm_name}

# Create host-only adapter
# ---
# Create new adapter and extract interface name from last line of output
if_name=$(VBoxManage hostonlyif create | tail -n1 | sed "s/Interface '\(.*\)' was.*/\1/")
# Extract relevant block of host data and find IP address in final line
if_ip=$(VBoxManage list hostonlyifs | sed "/Name: *${if_name}/,/IPAddress/ !d" | awk 'END{ print $NF }')
# Set appropriate relative IPs
sv_ip=$(echo ${if_ip} | awk 'BEGIN{OFS=FS="."}{$4++; print}')
sv_rng=$(echo ${if_ip} | awk 'BEGIN{OFS=FS="."}{$4=100; print}')
# Create and configure corresponding DHCP server
VBoxManage dhcpserver add --ifname ${if_name} --ip ${sv_ip} --netmask 255.255.255.0 --lowerip ${sv_rng} --upperip ${sv_rng}
VBoxManage dhcpserver modify --ifname "$if_name" --enable

# Configure network setting
VBoxManage modifyvm ${vm_name} --nic1 hostonly --hostonlyadapter1 ${if_name}

# Start appliance
VBoxManage startvm "$vm_name" --type headless

expect -c 'spawn ssh -o StrictHostKeyChecking=no admin@'${sv_rng}'; expect "Password"; send "admin\r"; expect "solace>"'

echo "The Solace instance is now available on $sv_rng"
