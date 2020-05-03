#define _LARGEFILE64_SOURCE

#include <iostream>
#include <jni.h>
#include <string>
#include <dlfcn.h>
#include <fcntl.h>
#include <sys/types.h>
#include <unistd.h>
#include <sys/mman.h>
#include <assert.h>
#include <linux/ptrace.h>
#include <sys/ptrace.h>
#include <stdio.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <vector>
#include <algorithm>
#include <dirent.h>
#include <string.h>
#include <map>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/system_properties.h>
#include "main.h"
#include <sstream>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <errno.h>
#include <unistd.h>
#include <fcntl.h>
#include <iomanip>
#include "Utils.h"

#define MYPORT  8688
#define QUEUE 20//连接请求队列

using namespace std;

int conn;
int ss;


constexpr char litte_sig[] = {0x04, 0xF0, 0x1F, 0xE5};

constexpr char big_sig[] = {0xE5, 0x1F, 0xF0, 0x04};

/**
 *
 * @param d1 起始地址
 * @param d2 特征码
 * @param mask
 * @param len 比较长度
 * @return
 */
inline bool compare(const char *d1, const char *d2, int len) {
    for (intptr_t i = 0; i < len; i++) {
        if ((uint8_t) d1[i] != (uint8_t) d2[i]) {
            return false;
        }
    }
    return true;
}


string dec2hex(int i) //将int转成16进制字符串
{
    stringstream ioss; //定义字符串流
    string s_temp; //存放转化后字符
    ioss << setiosflags(ios::uppercase) << hex << i; //以十六制(大写)形式输出
    //ioss << resetiosflags(ios::uppercase) << hex << i; //以十六制(小写)形式输出//取消大写的设置
    ioss >> s_temp;
    return s_temp;
}


/**
 *
 * @param start 查找hex特征码的起始地址
 * @param size  查找的范围大小
 * @param sig   特征码
 * @param cmpLen 比较的长度
 * @return 返回地址
 */
inline int
tryFind(int pid, void *start, void *end, const char *_sig, int cmpLen) {
    char memPath[256] = {0};
    size_t length = (uintptr_t) end - (uintptr_t) start;
    char *mem = (char *) malloc(length);
    memset(mem, 0, length);
    sprintf(memPath, "/proc/%d/mem", pid);
    int fd = open64(memPath, O_RDONLY);
    lseek64(fd, 0, SEEK_SET);
    pread64(fd, mem, length, (off64_t) start);
    close(fd);

    string address;
    for (int i = 0; i < length - cmpLen; i++) {
        if (compare((const char *) &mem[i], _sig, cmpLen)) {
            address.append("0x");
            address.append(dec2hex(i));
            address.append(",");
        }
    }
    cout << address << endl;
    if (address.size() > 5) {
        send(conn, address.c_str(), address.size(), 0);
    }
    return address.size();
}


/**
 * 通过包名获取pid
 * @param packageName
 * @return
 */
int getPidByPackageName(char *packageName) {
    DIR *pDir = opendir("/proc");
    dirent *file;
    if (pDir) {
        rewinddir(pDir);
        while ((file = readdir(pDir)) != nullptr) {
            if (file) {
                //判断是不是 . 或者 .. 文件夹
                if (strcmp(file->d_name, ".") == 0 || strcmp(file->d_name, "..") == 0) {
                    continue;
                }
                char path[256] = {0};
                if (file->d_type == DT_DIR) {
                    sprintf(path, "/proc/%s/cmdline", file->d_name);
                    FILE *pfile = fopen(path, "r");
                    if (pfile) {
                        char *process_name = (char *) malloc(128);
                        memset(process_name, 0, 128);
                        fgets(process_name, 128, pfile);
                        fclose(pfile);
                        if (strcmp(process_name, "") && !strstr(process_name, "su") &&
                            !strstr(process_name, "logcat") && strcmp(process_name, packageName) == 0) {
                            int pid = atoi(file->d_name);
                            if (pid) {
                                return pid;
                            }
                        }

                    }
                }
            }
        }
        return -1;
    } else {
        std::cout << "proc dir open failed" << std::endl;
    }
}

void dump(int pid, char *soName) {
    ProcMap map = getLibraryMap(pid, soName);
    char savePath[128] = {0};
    char memPath[256] = {0};
    char *mem = (char *) malloc(map.length);
    memset(mem, 0, map.length);
    sprintf(memPath, "/proc/%d/mem", pid);
    int fd = open64(memPath, O_RDONLY);
    lseek64(fd, 0, SEEK_SET);
    pread64(fd, mem, map.length, (off64_t) map.startAddr);
    close(fd);

    sprintf(savePath, "/sdcard/%s", soName);
    int fout = open64(savePath, O_WRONLY | O_CREAT, 0666);
    int ret=pwrite64(fout, mem, map.length, 0);
    close(fout);
    free(mem);
    if(ret==-1){
        const char* result="dump失败";
        send(conn,result,1024,0);
    } else{
        char result[1024] = {0};
        sprintf(result, "dump成功,保存在%s", savePath);
        send(conn,result,1024,0);
    }

//    if (ret == 0) {
//        char result[1024] = {0};
//        sprintf(result, "dump成功,保存在%s", savePath);
//        send(conn, result, 1024, 0);
//    } else {
//        const char *result = "dump失败";
//        send(conn, result, strlen(result), 0);
//    }

}

void findHookPoint(int pid, char *soName) {
    ProcMap map = getLibraryMap(pid, soName);
    int ret = tryFind(pid, map.startAddr, map.endAddr, litte_sig, strlen(litte_sig));
    if (ret < 5) {
        tryFind(pid, map.startAddr, map.endAddr, big_sig, strlen(big_sig));
    }

}

void socket_init() {
    ss = socket(AF_INET, SOCK_STREAM, 0);//若成功则返回一个sockfd（套接字描述符）
    //printf("%d\n",ss);
    struct sockaddr_in server_sockaddr;//一般是储存地址和端口的。用于信息的显示及存储使用
    /*设置 sockaddr_in 结构体中相关参数*/
    server_sockaddr.sin_family = AF_INET;
    server_sockaddr.sin_port = htons(MYPORT);//将一个无符号短整型数值转换为网络字节序，即大端模式(big-endian)　
    //printf("%d\n",INADDR_ANY);
    //INADDR_ANY就是指定地址为0.0.0.0的地址，这个地址事实上表示不确定地址，或“所有地址”、“任意地址”。
    //一般来说，在各个系统中均定义成为0值。
    server_sockaddr.sin_addr.s_addr = htonl(INADDR_ANY);//将主机的无符号长整形数转换成网络字节顺序。　
    if (bind(ss, (struct sockaddr *) &server_sockaddr, sizeof(server_sockaddr)) == -1) {
        perror("bind");
        exit(1);
    }
    if (listen(ss, QUEUE) == -1) {
        perror("listen");
        exit(1);
    }

    struct sockaddr_in client_addr;
    socklen_t length = sizeof(client_addr);
    ///成功返回非负描述字，出错返回-1
    conn = accept(ss, (struct sockaddr *) &client_addr, &length);
    //如果accpet成功，那么其返回值是由内核自动生成的一个全新描述符，代表与所返回客户的TCP连接。
    //accpet之后就会用新的套接字conn
    if (conn < 0) {
        perror("connect");
    }
    char buffer[1024];
    while (1) {
        memset(buffer, 0, sizeof(buffer));
        int len = recv(conn, buffer, sizeof(buffer), 0);//从TCP连接的另一端接收数据。
        /*该函数的第一个参数指定接收端套接字描述符；
        第二个参数指明一个缓冲区，该缓冲区用来存放recv函数接收到的数据；
        第三个参数指明buf的长度；
        第四个参数一般置0*/
        if (len > 0) {
            cout << buffer << endl;
            int mes;
            char packageName[64] = {0};
            char soName[32] = {0};
            sscanf(buffer, "%d\t%s\t%s", &mes, packageName, soName);
            //std::cout<<packageName<<","<<soName<<std::endl;
            int pid = getPidByPackageName(packageName);
            if (pid != -1) {
                switch (mes) {
                    case FINDHOOKADDRESS:
                        findHookPoint(pid, soName);
                        break;
                    case DUMP:
                        dump(pid, soName);
                        break;
                }
            }
        } else {
            conn = accept(ss, (struct sockaddr *) &client_addr, &length);
        }
    }

}

int main() {
    socket_init();
    return 0;
}