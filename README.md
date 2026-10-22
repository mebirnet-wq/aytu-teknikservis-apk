# aytu-teknikservis-apk
AYTU Bobinaj teknik servis Android ve iOS uygulaması

# AYTU Bobinaj Mobil

Android ve iOS için Flutter tabanlı AYTU Bobinaj teknik servis uygulamasının başlangıç projesi.

## Marka logosu
Giriş ekranında AYTU Servis Pazarı logosu aşağıdaki adresten gösterilir:

`https://aytubobinaj.com/wp-content/uploads/2026/10/cropped-AYTU-Servis-Pazari-Ikonu.png`

Bu kullanım için internet bağlantısı gerekir. Android/iOS ana ekran uygulama simgesine logoyu yerleştirmek için PNG dosyasının proje içinde yerel asset olarak bulunması ve platform ikonlarının ayrıca üretilmesi gerekir. Bu çalışma ortamında görsel dosyasını URL’den indirme erişimi olmadığı için yerel ikon dosyası henüz pakete eklenmemiştir.

## Bu başlangıç sürümünde
- Türkçe giriş ekranı taslağı
- Yönetici / teknisyen demo görünümü seçimi
- Servis özeti ve örnek servis kartları
- Mobil uyumlu Material 3 arayüzü

> Önemli: Bu sürüm arayüz prototipidir. Kimlik doğrulama, canlı servis verileri ve WordPress API bağlantısı henüz eklenmemiştir. Ekranda görünen kayıtlar örnek veridir. Üretimde kullanmadan önce yetkilendirme ve API bağlantısı kurulmalıdır.

## Gereksinimler
- Flutter SDK (stable)
- Android Studio (Android SDK ve emülatör için)
- iOS derlemesi için macOS ve Xcode

## Çalıştırma

```bash
flutter doctor
flutter pub get
flutter run
```

## GitHub'a yükleme
Depoyu GitHub'da `aytu-bobinaj-mobil` adıyla oluşturduktan sonra bu klasörde:

```bash
git init
git add .
git commit -m "İlk Flutter uygulama iskeleti"
git branch -M main
git remote add origin https://github.com/KULLANICI-ADIN/aytu-bobinaj-mobil.git
git push -u origin main
```

`KULLANICI-ADIN` bölümünü kendi GitHub kullanıcı adınızla değiştirin.

## Sonraki geliştirme adımları
1. Mevcut WordPress eklentisinin servis kayıtlarını ve kullanıcı rollerini incelemek.
2. WordPress REST API uç noktalarını ve güvenli kimlik doğrulamayı tasarlamak.
3. Demo servis listesini canlı API ile değiştirmek.
4. Servis detayları, durum güncelleme ve teknik not ekleme ekranlarını yapmak.
5. Android ve iOS cihazlarında test etmek.
