# Quality Assurance - Modul Pengiriman

Dokumen ini merangkum evidence dan quality effort untuk modul Pengiriman yang dikerjakan oleh Kadek Ngurah Septyawan Chandra Diputra pada branch `feat/pengiriman`.

## Scope Implementasi

Modul Pengiriman mencakup alur utama berikut:

- Mandor melihat dan memfilter supir yang berada pada kebun yang dikelola.
- Mandor menugaskan supir ke satu atau beberapa panen approved dengan batas maksimum 400 kg.
- Mandor menggunakan rekomendasi assignment berbasis knapsack untuk memilih kombinasi panen.
- Supir melihat daftar pengiriman miliknya dan memperbarui status pengiriman.
- Mandor memantau pengiriman aktif serta approve atau reject pengiriman yang sudah tiba.
- Admin memproses pengiriman yang sudah disetujui mandor melalui approve penuh, reject, atau partial accept.
- Modul Pengiriman mem-publish event untuk integrasi asynchronous dengan modul pembayaran dan notifikasi.

## CI/CD Evidence

| Evidence | File |
| --- | --- |
| Final PR Pengiriman merged ke `staging` | [`01-pr-final-merged-to-staging.png`](pengiriman-evidence/01-pr-final-merged-to-staging.png) |
| Backend CI/CD pada branch `staging` berhasil, termasuk `ci / Run tests` dan `cd / cd-staging` | [`02-backend-ci-cd-staging-success.png`](pengiriman-evidence/02-backend-ci-cd-staging-success.png) |
| Frontend CI/CD pada branch `staging` berhasil, termasuk CI dan `cd / cd-staging` | [`03-frontend-ci-cd-staging-success.png`](pengiriman-evidence/03-frontend-ci-cd-staging-success.png) |

Kedua repository sudah menjalankan workflow pada branch `staging` setelah PR Pengiriman di-merge. Job `cd-main` skipped karena branch yang diproses adalah `staging`, sedangkan job `cd-staging` berhasil.

## Test, Coverage, Build, and Lint Evidence

| Evidence | File |
| --- | --- |
| Backend package coverage Pengiriman 100% | [`04-backend-coverage-100.png`](pengiriman-evidence/04-backend-coverage-100.png) |
| Frontend module coverage Pengiriman 100% dengan 59 tests passed | [`05-frontend-coverage-100.png`](pengiriman-evidence/05-frontend-coverage-100.png) |
| Backend local build berhasil | [`06-backend-build-success.png`](pengiriman-evidence/06-backend-build-success.png) |
| Frontend lint selesai dengan 0 errors | [`07-frontend-lint-0-errors.png`](pengiriman-evidence/07-frontend-lint-0-errors.png) |
| Frontend production build berhasil | [`08-frontend-build-success.png`](pengiriman-evidence/08-frontend-build-success.png) |

Catatan untuk lint frontend: warning yang tersisa berasal dari penggunaan `<img>` pada modul Panen, bukan dari modul Pengiriman. Lint tetap selesai dengan `0 errors`.

## Security and Quality Controls

### Role-Based Access Control

Endpoint Pengiriman di backend dibatasi berdasarkan role menggunakan method-level authorization:

- `MANDOR` dapat melakukan assignment, melihat supir kebun, melihat panen assignable, melihat rekomendasi assignment, memantau pengiriman aktif, serta approve atau reject pengiriman.
- `SUPIR` dapat melihat pengiriman miliknya dan memperbarui status pengiriman.
- `ADMIN` dapat melihat pengiriman yang sudah disetujui mandor dan memproses hasil akhir pengiriman.

Implementasi utama berada pada:

- `src/main/java/id/ac/ui/cs/advprog/mysawitbe/modules/pengiriman/infrastructure/web/PengirimanController.java`

### Backend Business Rule Validation

Validasi penting diterapkan di backend, sehingga tidak bergantung pada validasi frontend saja:

- Mandor dan supir wajib valid.
- Supir yang dipilih harus terdaftar pada kebun mandor.
- Panen yang ditugaskan harus approved dan berasal dari kebun mandor.
- Panen yang sudah masuk ke pengiriman lain tidak dapat ditugaskan ulang.
- Total berat pengiriman harus lebih dari 0 dan tidak boleh melebihi 400 kg.
- Supir hanya dapat melakukan transisi status `ASSIGNED -> IN_TRANSIT -> TIBA`.
- Mandor hanya dapat approve atau reject pengiriman miliknya yang sudah `TIBA`.
- Reject oleh mandor wajib memiliki alasan.
- Admin hanya dapat memproses pengiriman dengan status `APPROVED_MANDOR`.
- Admin full approve wajib menerima seluruh berat pengiriman.
- Admin partial accept wajib memiliki berat diterima yang valid dan alasan.
- Admin reject wajib memiliki `acceptedWeight` 0 dan alasan.

Implementasi utama berada pada:

- `src/main/java/id/ac/ui/cs/advprog/mysawitbe/modules/pengiriman/application/service/PengirimanCommandUseCaseImpl.java`

### Ownership Validation

Operasi sensitif memvalidasi ownership sebelum perubahan data dilakukan:

- Supir hanya dapat mengubah status pengiriman yang ditugaskan kepadanya.
- Mandor hanya dapat approve atau reject pengiriman yang berada pada tanggung jawabnya.
- Panen assignable dan supir list diambil berdasarkan kebun yang dikelola mandor.

### Assignment Recommendation

Fitur optional `recommend assignment` menggunakan pendekatan 0/1 knapsack untuk memilih kombinasi panen yang memaksimalkan total berat tanpa melewati kapasitas maksimum. Secara default kapasitas maksimum adalah 400 kg.

Implementasi utama berada pada:

- `src/main/java/id/ac/ui/cs/advprog/mysawitbe/modules/pengiriman/application/service/PengirimanQueryUseCaseImpl.java`

### Asynchronous Integration

Modul Pengiriman mem-publish event untuk integrasi asynchronous:

- `PengirimanStatusTibaEvent`
- `PengirimanApprovedByMandorEvent`
- `PengirimanProcessedByAdminEvent`

Event tersebut digunakan oleh modul lain, termasuk pembayaran dan notifikasi, sehingga alur Pengiriman tetap terintegrasi dengan project kelompok.

## Verification Commands

Backend:

```powershell
cd D:\UI\TahunKedua\Semester4\Adpro\TK_Adpro\mysawit-be
.\gradlew.bat test --tests "id.ac.ui.cs.advprog.mysawitbe.modules.pengiriman.*"
.\gradlew.bat build
```

Frontend:

```powershell
cd D:\UI\TahunKedua\Semester4\Adpro\TK_Adpro\mysawit-fe
pnpm exec vitest run src/modules/pengiriman --coverage --coverage.reporter=text
pnpm lint
pnpm build
```

## Summary

Berdasarkan evidence yang tersedia, modul Pengiriman sudah memenuhi:

- Implementasi fitur yang terintegrasi dengan backend dan frontend.
- CI/CD successful pada branch `staging` untuk backend dan frontend.
- Unit test dan coverage 100% untuk scope Pengiriman.
- Build backend dan frontend berhasil.
- Role-based access control dan backend business rule validation untuk menjaga keamanan dan konsistensi data.
