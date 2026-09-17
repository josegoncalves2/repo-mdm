import sys
from pathlib import Path

# Add parent directory to path so we can import modules from webfilter-dns/app
sys.path.insert(0, str(Path(__file__).parent.parent / "app"))
