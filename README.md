# Contoh Implementasi Arsitektur Spring Boot

Repository ini berisi implementasi berbagai arsitektur dari aplikasi yang sama menggunakan Spring Boot dan WebFlux.

---

## Branch yang Tersedia

### 1. **Branch: `ddd`**
Implementasi Domain-Driven Design

**Pindah ke branch ini:**
```bash
git checkout ddd
```

**Isi:**
- Arsitektur Domain-Driven Design
- Aggregate pattern
- Repository pattern
- Value Objects
- Domain Events

---

### 2. **Branch: `hexagonal`** (Disarankan)
Arsitektur Hexagonal Murni (Ports & Adapters)

**Pindah ke branch ini:**
```bash
git checkout hexagonal
```

**Isi:**
- Hexagonal Architecture (Netflix style)
- Ports & Adapters pattern
- Pemisahan concern yang jelas
- Mapping DTO ↔ Domain di adapter
- Reactive programming dengan WebFlux
- BlockHound untuk validasi non-blocking
- Dokumentasi lengkap

**Dokumentasi:**
- PURE_HEXAGONAL_ARCHITECTURE.md - Konsep dasar
- HEXAGONAL_MAPPING_GUIDE.md - Pattern mapping
- COMPOSITE_ADAPTER_GUIDE.md - Multiple providers
- DI_INJECTION_GUIDE.md - Dependency injection
- TDD_HEXAGONAL_WORKFLOW.md - Test-driven development
- REACTIVE_GUIDE.md - Reactive programming
- BLOCKHOUND_GUIDE.md - Validasi non-blocking

---

## Perbandingan

| Aspek | DDD | Hexagonal |
|-------|-----|-----------|
| Fokus | Domain model | Port boundaries |
| Struktur | Aggregates | Ports & Adapters |
| Dependencies | Domain → Infrastructure | Infrastructure → Application → Domain |
| Mapping | Service layer | Adapter layer |
| Cocok untuk | Business logic kompleks | Sistem fleksibel & testable |

---

## Cara Mulai

1. Pilih arsitektur yang diinginkan:
   - `git checkout ddd` - Untuk pendekatan DDD
   - `git checkout hexagonal` - Untuk pendekatan Hexagonal

2. Baca README di branch tersebut untuk instruksi detail

3. Jalankan aplikasi:
   ```bash
   mvn spring-boot:run
   ```

---

## Tujuan Project

Belajar dan membandingkan berbagai pattern arsitektur dengan implementasi aplikasi yang sama:
- User management
- Post management
- Integrasi external API
- Reactive programming
- Clean code principles

---

**Pilih branch di atas untuk memulai!**
