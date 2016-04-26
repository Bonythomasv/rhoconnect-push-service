#**RhoConnect Push Service for Windows Desktop Environment**
This repository consists the code for **rhoconnect-push-service** for **Windows Desktop** environment.

###**RhoConnect Push Service Pre-requisite:**
1. Follow the **[Windows Desktop Pre-requisite guidelines](https://github.com/rhomobile/rhodes/blob/master/doc/oss/Windows_Desktop_Installation_And_Build_Guidelines.md)**.
2. Clone **```rhoconnect-push-service```** repository into the local machine.
3. **Windows Desktop** code for **RhoConnect Push Service** is present under **```<PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\win32```** location after cloning the **```rhoconnect-push-service```** repository into the local machine. The github repository link is [rhoconnect-push-service](https://github.com/rhomobile/rhoconnect-push-service.git).

###**RhoConnect Push Service Building Procedure:**
1. If the pre-requisite is properly set, goto  **```<PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\win32\ANS```** location and open the project **```ANS.sln```** using **Microsoft Visual Studio 2012**.

2. After opening the project, build the project in order which are listed below. The below project will be visible after opening the **```ANS.sln```** using Microsoft Visual Studio 2012.
   - ```ANS``` - <i>Ensure to build this project in **Release** mode.</i>
   - ```rhoconnectpush``` - <i>Ensure to build this project in **CD_ROM** or **DVD-5** or **SingleImage** mode.</i> 
 
 Both the above project are included in ```ANS``` project solution.

3. If both the project were build in proper order as listed above through **Microsoft Visual Studio 2012** will generate the output inside **```<PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\win32\ANS\rhoconnect-push-service\Express```** location if and only if the build was successful.
    
   **<i>Note:</i>**
     - If the ```rhoconnectpush``` project was build using **CD_ROM** mode, then the output for successful build will present under  **```<PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\win32\ANS\rhoconnect-push-service\Express\CD_ROM```** location.
     - If the ```rhoconnectpush``` project was build using **DVD-5** mode, then the output for successful build will present under  **```<PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\win32\ANS\rhoconnect-push-service\Express\DVD-5```** location.
     - If the ```rhoconnectpush``` project was build using **SingleImage** mode, then the output for successful build will present under  **```<PATH_TO_RHOCONNECT_PUSH_REPOSITORY>\rhoconnect-push-service\win32\ANS\rhoconnect-push-service\Express\SingleImage```** location.
