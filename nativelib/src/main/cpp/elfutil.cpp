#include <cstdint>
#include <cstdio>
#include <cstring>
#include "mylog.h"
#include <cinttypes>
#include <elf.h>
#include <malloc.h>
#include "elfutil.h"

void getFullPath(const char *src, char *dest) {
    while (*src != '/') {
        *src++;
    }
    strncpy(dest, src, strlen(src) - 1);
}

uintptr_t getModuleBase(const char *module_name, char *moduleFullPath) {
    uintptr_t addr = 0;
    char buff[256] = "\n";

    FILE *fp = fopen("/proc/self/maps", "r");
    while (fgets(buff, sizeof(buff), fp)) {
        if (strstr(buff, "r-xp") && strstr(buff, module_name) &&
            sscanf(buff, "%" SCNxPTR, &addr) == 1) {
            getFullPath(buff, moduleFullPath);
            LOGE("[%s] moduleBase: %" SCNxPTR, moduleFullPath, addr);
            return addr;
        }
    }
    LOGE("[%s] moduleBase not found!\n", module_name);
    fclose(fp);
    return 0;
}

int getGOTOffsetAndSize(const char *modulePath, int &GOTSize) {
    int GOTOffset = 0;
    FILE *fp = fopen(modulePath, "r");
    if (!fp) {
        LOGE("[%s] open failed!", modulePath);
        return 0;
    }
    ELFW(Ehdr) elf_header;
    ELFW(Shdr) elf_section_header;
    memset(&elf_header, 0, sizeof(elf_header));
    memset(&elf_section_header, 0, sizeof(elf_section_header));
    // 解析elf_header
    fseek(fp, 0, SEEK_SET);
    fread(&elf_header, sizeof(elf_header), 1, fp);
    // 获取字符串表在section header中的偏移
    fseek(fp, elf_header.e_shoff + elf_header.e_shstrndx * elf_header.e_shentsize, SEEK_SET);
    fread(&elf_section_header, sizeof(elf_section_header), 1, fp);
    int string_table_size = elf_section_header.sh_size;
    char *string_table = (char *) (malloc(string_table_size));
    // 获取字符串表
    fseek(fp, elf_section_header.sh_offset, SEEK_SET);
    fread(string_table, string_table_size, 1, fp);
    // 遍历section header table, 查找.got
    fseek(fp, elf_header.e_shoff, SEEK_SET);
    for (int i = 0; i < elf_header.e_shnum; ++i) {
        fread(&elf_section_header, elf_header.e_shentsize, 1, fp);
        if (elf_section_header.sh_type == SHT_PROGBITS
            && 0 == strcmp(".got", string_table + elf_section_header.sh_name)) {
            GOTOffset = elf_section_header.sh_addr;
            GOTSize = elf_section_header.sh_size;
            break;
        }
    }
    free(string_table);
    fclose(fp);
    return GOTOffset;
}