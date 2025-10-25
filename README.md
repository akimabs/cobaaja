# Spring Boot Architecture Examples

Repository ini berisi implementasi berbagai arsitektur dari aplikasi yang sama.

---

## Branch yang Tersedia

### Branch: `ddd`
Domain-Driven Design dengan Repository pattern

```bash
git checkout ddd
```

**Karakteristik:**
- Repository interface di Domain layer
- Service inject Repository
- Domain model sebagai center
- Adapter untuk external system

---

### Branch: `hexagonal`
Hexagonal Architecture dengan Ports & Adapters

```bash
git checkout hexagonal
```

**Karakteristik:**
- Port interface di Application layer
- Service inject Port
- Application boundary yang jelas
- Adapter untuk external system
- Dokumentasi lengkap

---

## Perbedaan Utama

| DDD | Hexagonal |
|-----|-----------|
| Repository (di Domain) | Port (di Application) |
| Domain-centric | Boundary-centric |

**Kesamaan:** Sama-sama pakai Adapter di Infrastructure layer

---

## Quick Start

```bash
# Pilih branch
git checkout ddd        # atau
git checkout hexagonal

# Jalankan
mvn spring-boot:run

# Test
curl http://localhost:8080/api/users/1
```

---

Pilih branch di atas untuk lihat implementasi lengkap!
