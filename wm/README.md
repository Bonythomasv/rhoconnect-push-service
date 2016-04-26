#**RhoConnect Push Service for Windows Mobile/CE Environment**
This repository consists the code for **rhoconnect-push-service** for **Windows Mobile/CE** environment.

###**RhoConnect Push Service Pre-requisite:**
1. Follow the **[Windows Mobile/CE Pre-requisite guidelines](https://github.com/rhomobile/rhodes/blob/master/doc/oss/WM_CE_Installation_And_Build_Guidelines.md)**.
2. Clone **```rhoconnect-push-service```** repository into the local machine.
3. **Windows Mobile/CE** code for **RhoConnect Push Service** is present under **```<PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\wm```** location after cloning the **```rhoconnect-push-service```** repository into the local machine. The github repository link is [rhoconnect-push-service](https://github.com/rhomobile/rhoconnect-push-service.git).
4. Download & Install **[SED](https://sourceforge.net/projects/gnuwin32/files/sed/4.2.1/sed-4.2.1-setup.exe/download)** i.e. ```sed-4.2.1-setup.exe```. After installation, the location of ```Sed.exe``` should be added to the System ```Path``` environment variable. 
   
 **Note:**
   - On **Windows 7 64 bit machine**, the default installation happens at **```C:\Program Files (x86)\GnuWin32\bin\```** location. One can install & verify & can add the same to the System ```Path``` environment variable.
   - **SED** is used for generating **```rhoconnect-push-service.CAB```** file using the batch file i.e. **```build.bat```** which is available under **```<PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\wm```** location after cloning the **```rhoconnect-push-service```** repository into the local machine. The batch file internally uses the **SED** command for generating the version detail for **rhoconnect-push-service**.

###**RhoConnect Push Service Building Procedure:**
1. If the pre-requisite is properly set, goto  **```<PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\wm```** location and run **```build.bat```** from command prompt.
2. After successful build, the **```rhoconnect-push-service.CAB```** will be available at **``` <PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\wm\rhoconnect-push-service```** location in the machine. 

###**RhoConnect Push Service Installation On Windows Mobile/CE Device:**
**RhoConnect Push Service** application i.e. **```rhoconnect-push-service.CAB```** service must be installed in **Windows Mobile/CE** device.
 - Before installing the **RhoConnect Push Service** application i.e. **```rhoconnect-push-service.CAB```** on **Windows Mobile** devices, one must need to install the **```.NET compact framework```** on your device. 
     - **NETCFv35.Messages.EN.cab**
     - **NETCFv35.Messages.EN.wm.cab**
     
    You may find the device installation package on your build machine at **```C:\Program Files (x86)\Microsoft.NET\SDK\CompactFramework\v3.5\WindowsCE\Diagnostics\```** on **Windows 7 machine**.

###**Build Notes:**
- User can build and generate the **```rhoconnect-push-service.CAB```** file using **```ANS.sln```**project from **Visual Studio 2008** if user doesnot want to build and generate the **```rhoconnect-push-service.CAB```** using the batch file. The solution file is available at **``` <PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\wm\ANS.sln```**
- User can also set the required version detail for creating the **CAB** file using the batch file. Detail is mentioned below. The default version which is currently set is **1.0.0.2**.
```
A> If user want to build particular version then set version parameter before running build.bat.
B> To set version run "set version=X.X.X.X" from command line. Default version is 1.0.0.2.

Example: 
Open command prompt and navigate to the location where build.bat is present in the local machine and then type as mentioned below.
1. set version=1.0.0.3 //Hit enter, if user want to set the version as 1.0.0.3
2. build.bat //Hit enter for generating the rhoconnect-push-service.CAB
```
