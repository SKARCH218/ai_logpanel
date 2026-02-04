#!/usr/bin/env python3
"""
PNG를 ICO로 변환하는 스크립트
Pillow 라이브러리 필요: pip install Pillow
"""

try:
    from PIL import Image
    import os

    input_file = "composeApp/src/jvmMain/resources/logo.png"
    output_file = "composeApp/src/jvmMain/resources/logo.ico"

    print(f"🔄 PNG를 ICO로 변환 중...")
    print(f"   입력: {input_file}")
    print(f"   출력: {output_file}")

    # PNG 열기
    img = Image.open(input_file)

    # RGBA 모드로 변환
    if img.mode != 'RGBA':
        img = img.convert('RGBA')

    # 여러 크기의 아이콘 생성 (Windows 권장)
    icon_sizes = [(16, 16), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)]

    # ICO 파일로 저장
    img.save(output_file, format='ICO', sizes=icon_sizes)

    print(f"✅ 변환 완료!")
    print(f"   생성된 파일: {output_file}")
    print(f"   크기: {os.path.getsize(output_file) / 1024:.2f} KB")

except ImportError:
    print("❌ Pillow 라이브러리가 설치되지 않았습니다.")
    print("")
    print("설치 방법:")
    print("   pip install Pillow")
    print("")
    print("또는 온라인 변환 도구를 사용하세요:")
    print("   https://convertio.co/kr/png-ico/")

except Exception as e:
    print(f"❌ 오류 발생: {e}")
