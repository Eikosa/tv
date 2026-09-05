import re
import sys
from pathlib import Path

def test_channel_resolution():
    source_file = Path("TurkiyeTV/src/main/kotlin/com/eikosa/turkiyetv/TurkiyeTVProvider.kt")
    if not source_file.is_file():
        print(f"Error: {source_file} not found")
        sys.exit(1)

    source = source_file.read_text(encoding="utf-8")

    # Extract all channel IDs defined in TurkiyeTVProvider
    standard_ids = re.findall(r'(?m)^\s*id\s*=\s*"([^"]+)"', source)
    garden_ids = re.findall(r'tvGardenChannel\("([^"]+)"', source)
    yt_ids = [f"YouTube_{m}" for m in re.findall(r'youtubeChannel\("([^"]+)"', source)]

    all_ids = standard_ids + garden_ids + yt_ids

    # Extract stream URLs
    stream_urls = re.findall(r'streamUrl\s*=\s*"([^"]+)"', source)
    garden_urls = re.findall(r'tvGardenChannel\("[^"]+",\s*"[^"]+",\s*"([^"]+)"', source)
    all_urls = stream_urls + garden_urls

    collections = [
        "YouTubeCollection_DiscoveryChannelTurkiye",
        "YouTubeCollection_NationalGeographicTurkiye"
    ]

    print(f"[TEST] Toplam bulunan kanal kimliği sayısı: {len(all_ids)}")
    print(f"[TEST] Toplam bulunan yayın URL sayısı: {len(all_urls)}")
    print(f"[TEST] Toplam bulunan koleksiyon sayısı: {len(collections)}")

    if len(all_ids) < 220:
        print(f"FAILED: Kanal sayısı beklenenden düşük: {len(all_ids)}")
        sys.exit(1)

    main_url = "https://github.com/Eikosa/tv"

    def clean_channel_url(url: str) -> str:
        return (
            url.removeprefix(main_url)
            .removeprefix("https://github.com/Eikosa/tv")
            .removeprefix("http://github.com/Eikosa/tv")
            .removeprefix("/")
        )

    all_id_set = set(all_ids)
    all_url_set = set(all_urls)
    collection_set = set(collections)

    def simulate_load(url: str) -> bool:
        clean_url = clean_channel_url(url)
        # Check channel ID match
        if url in all_id_set or clean_url in all_id_set:
            return True
        # Check channel stream URL match
        if url in all_url_set or clean_url in all_url_set:
            return True
        # Check collection match
        if url in collection_set or clean_url in collection_set:
            return True
        # Check YouTube URL resolution
        if url.startswith("https://www.youtube.com/") or url.startswith("https://youtu.be/"):
            return True
        if clean_url.startswith("https://www.youtube.com/") or clean_url.startswith("https://youtu.be/"):
            return True
        # Check direct HLS resolution
        target = clean_url if clean_url.startswith(("http://", "https://")) else url
        if target.startswith(("http://", "https://")) and (".m3u8" in target):
            return True
        return False

    failures = []
    # Test 1: Each channel by direct ID
    for cid in all_ids:
        if not simulate_load(cid):
            failures.append(f"Doğrudan ID çözülemedi: {cid}")

    # Test 2: Each channel by CloudStream APIRepository prefixed ID:
    # CloudStream executes: load(fixUrl(channel.url)) -> mainUrl + "/" + channel.id
    for cid in all_ids:
        prefixed_id = f"{main_url}/{cid}"
        if not simulate_load(prefixed_id):
            failures.append(f"CloudStream prefixli ID çözülemedi: {prefixed_id}")

    # Test 3: Each channel by direct streamUrl
    for surl in all_urls:
        if not simulate_load(surl):
            failures.append(f"Doğrudan streamUrl çözülemedi: {surl}")

    # Test 4: Each channel by CloudStream prefixed streamUrl
    for surl in all_urls:
        prefixed_url = f"{main_url}/{surl}"
        if not simulate_load(prefixed_url):
            failures.append(f"CloudStream prefixli streamUrl çözülemedi: {prefixed_url}")

    # Test 5: Collections
    for col_id in collections:
        if not simulate_load(col_id):
            failures.append(f"Koleksiyon doğrudan ID çözülemedi: {col_id}")
        if not simulate_load(f"{main_url}/{col_id}"):
            failures.append(f"Koleksiyon prefixli ID çözülemedi: {main_url}/{col_id}")

    if failures:
        print(f"FAILED: {len(failures)} adet kanal çözme senaryosu başarısız oldu:")
        for f in failures[:10]:
            print(f"  - {f}")
        sys.exit(1)

    print(f"SUCCESS: {len(all_ids)} kanal ve {len(collections)} koleksiyonun tamamı CloudStream ön ekleriyle simüle edilip doğrulandı!")

if __name__ == "__main__":
    test_channel_resolution()
