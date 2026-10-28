# Dinamik Kelime Puzzle Oyunu

> A Turkish grid-based word puzzle game for Android: find words in 8 directions, chain sub-word combos, buy jokers in an in-game market, and track scores with a Room database.

Kocaeli Üniversitesi Bilgisayar Mühendisliği, **Yazılım Laboratuvarı II – Proje 2** kapsamında geliştirilmiş bir Android oyunu. Oyuncu harf ızgarasında komşu harfleri birleştirerek Türkçe kelimeler bulur. İçinde başka kelimeler barındıran kelimelerle kombo puanı kazanır.

## Özellikler

- **Ayarlanabilir ızgara:** 6×6, 8×8 veya 10×10 oyun alanı.
- **Türkçe sözlük:** 63.000'den fazla kelimelik sözlükle anlık kelime doğrulama. Puanlamada Türkçe büyük/küçük harf kurallarına uyulur.
- **Ağırlıklı harf üretimi:** Harfler kullanım sıklıklarına göre ağırlıklı bir havuzdan üretilir. Örneğin A, E, İ, L, R, N daha sık; J, Ğ, F, V daha seyrek gelir.
- **Kombo puanlama:** Bulunan kelimenin içindeki 3 harften uzun tüm alt kelimeler taranır. Sözlükte geçen her alt kelime ek puan kazandırır.
- **Jokerler ve market:** Oyun parasıyla joker satın alınır ve oyun içinde kullanılır. Joker çeşitleri: Balık, Tekerlek, Lolipop Kırıcı, Serbest Değiştirme, Harf Karıştırma, Parti Güçlendiricisi.
- **Kullanıcı hesapları ve skor tablosu:** Giriş yapan kullanıcıların oyunları, skorları ve jokerleri Room veritabanında saklanır.
- **Oyun deneyimi:**
  - Zamanlayıcı ve oyun sonu özet ekranı.
  - Arka plan müziği ve hata sesleri.
  - Özel çizimli (Canvas) oyun tahtası.

## Algoritmalar

- **Kelime arama (DFS + backtracking):** Tahtadaki olası kelimeler 8 yönlü derinlik öncelikli aramayla bulunur. Sözlükte hiçbir kelimenin başlangıcı olmayan harf dizilimlerinde arama önek kontrolüyle erken kesilir.
- **Kesişmeyen kelime seçimi (greedy):** Ortak harf kullanmayan en fazla sayıda kelimeyi seçmek için adaylar kısadan uzuna sıralanır ve açgözlü bir yaklaşımla seçilir.
- **Kombo tespiti:** Seçilen kelimenin alt dizileri sözlükte aranır ve harf puanları toplanır.

## Teknolojiler

| Alan | Teknoloji |
| --- | --- |
| Dil | Java |
| Platform | Android SDK (minSdk 24, targetSdk 36) |
| Veritabanı | Room (SQLite) |
| Eşzamanlılık | Arka plan iş parçacıkları (`Thread`) ile veritabanı işlemleri, `Handler` ile zamanlayıcı |
| Arayüz | Özel `View` (Canvas), RecyclerView adaptörleri |
| Derleme | Gradle |

## Proje Yapısı

```
app/src/main/java/com/example/kelimeoyunu/
├── ui/         # GameActivity, GameBoardView, Market, Scoreboard, Login, Settings
├── database/   # Room: AppDatabase, AppDao, User/Game/Joker entity'leri
├── models/     # GridCell, JokerItem
├── adapters/   # Market ve skor listesi adaptörleri
└── utils/      # DictionaryManager, LetterGenerator, ScoreCalculator
app/src/main/assets/words.txt   # Türkçe sözlük
```

## Kurulum ve Çalıştırma

1. Depoyu klonlayın:

   ```bash
   git clone https://github.com/emirallak/Dinamik-Kelime-Oyunu.git
   ```

2. Projeyi **Android Studio** ile açın ve Gradle senkronizasyonunun bitmesini bekleyin.
3. Bir emülatör veya Android 7.0 (API 24) ve üzeri bir cihaz seçip **Run** ile çalıştırın.

## Geliştirici

**Muhammed Emir Allak**: [GitHub](https://github.com/emirallak) · [LinkedIn](www.linkedin.com/in/muhammed-emir-allak-a445a3246/)
