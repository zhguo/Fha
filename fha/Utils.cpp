

#include "Utils.h"


ProcMap getLibraryMap(int pid, const char *libraryName) {
    ProcMap retMap;
    char line[512] = {0};
    char mapPath[128] = {0};
    sprintf(mapPath, "/proc/%d/maps", pid);
    FILE *fp = fopen(mapPath, "rt");
    bool isFirst = true;
    void *tmp;
    int index=0;
    if (fp != NULL) {
        while (fgets(line, sizeof(line), fp)) {
            index++;
            if(index>100010){
                break;
            }
            if (strstr(line, libraryName)) {
                if (isFirst) {
                    char tmpPerms[5] = {}, tmpDev[12] = {}, tmpPathname[455] = {};
                    sscanf(line, "%llx-%llx %s %ld %s %d %s",
                           (long long unsigned *) &retMap.startAddr,
                           (long long unsigned *) &retMap.endAddr,
                           tmpPerms, &retMap.offset, tmpDev, &retMap.inode, tmpPathname);
                    retMap.perms = tmpPerms;
                    retMap.dev = tmpDev;
                    retMap.pathname = tmpPathname;
                    index=100000; //从100000开始
                    isFirst = false;
                } else {
                    sscanf(line, "%llx-%llx",
                           (long long unsigned *) &tmp,
                           (long long unsigned *) &retMap.endAddr);
                }
            }
        }
        retMap.length = (uint64_t) retMap.endAddr - (uint64_t) retMap.startAddr;
        fclose(fp);
    }
    return retMap;
}


std::string getProcName() {
    std::string ret;
    char cmdline[256] = {0};
    FILE *fp;
    fp = fopen("/proc/self/cmdline", "r");
    if (fp) {
        fgets(cmdline, sizeof(cmdline), fp);
        fclose(fp);
        ret = cmdline;
    }
    return ret;
}

std::string getSoName() {
    std::string ret;
    char soName[16] = {0};
    FILE *fp;
    fp = fopen("/sdcard/soName", "r");
    if (fp) {
        fgets(soName, sizeof(soName), fp);
        fclose(fp);
        ret = soName;
    }
    return ret;
}

bool getFileStat(const char *fileName, struct stat *buffer) {
    return stat(fileName, buffer) != -1;
}

bool isMainProcName() {
    return strstr(getProcName().c_str(), ":") == NULL;
}