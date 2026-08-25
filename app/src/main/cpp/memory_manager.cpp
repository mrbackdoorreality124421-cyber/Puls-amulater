#include <cstdlib>
#include <cstring>

size_t g_total_ram = 0;
size_t g_used_ram = 0;

void qemu_memory_init(size_t ram_size_mb) {
    g_total_ram = ram_size_mb * 1024 * 1024;
    g_used_ram = 0;
}

void* qemu_memory_alloc(size_t size) {
    if (g_used_ram + size > g_total_ram) {
        return nullptr; // Out of memory
    }
    
    void* ptr = malloc(size);
    if (ptr) {
        g_used_ram += size;
    }
    return ptr;
}

void qemu_memory_free(void* ptr) {
    if (ptr) {
        // In real implementation: track allocation size
        free(ptr);
    }
}
