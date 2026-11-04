(() => {
  const toggle = document.getElementById("menu-toggle");
  const menu = document.getElementById("mobile-menu");

  if (toggle && menu) {
    const closeMenu = () => {
      menu.hidden = true;
      toggle.setAttribute("aria-expanded", "false");
      toggle.setAttribute("aria-label", "Open menu");
      document.body.classList.remove("menu-open");
    };

    toggle.addEventListener("click", () => {
      const willOpen = menu.hidden;
      menu.hidden = !willOpen;
      toggle.setAttribute("aria-expanded", String(willOpen));
      toggle.setAttribute("aria-label", willOpen ? "Close menu" : "Open menu");
      document.body.classList.toggle("menu-open", willOpen);
    });

    menu.querySelectorAll("a").forEach((link) => link.addEventListener("click", closeMenu));
    document.addEventListener("keydown", (event) => {
      if (event.key === "Escape") closeMenu();
    });
    document.addEventListener("click", (event) => {
      if (!menu.hidden && !menu.contains(event.target) && !toggle.contains(event.target)) closeMenu();
    });
  }

  const snippets = {
    python: {
      file: "binary_search.py",
      code: [
        "def binary_search(items, target):",
        "    low, high = 0, len(items) - 1",
        "",
        "    while low <= high:",
        "        mid = (low + high) // 2",
        "        if items[mid] == target:",
        "            return mid",
        "        if items[mid] < target:",
        "            low = mid + 1",
        "        else:",
        "            high = mid - 1",
        "",
        "    return -1"
      ].join("\n")
    },
    kotlin: {
      file: "BinarySearch.kt",
      code: [
        "fun binarySearch(items: IntArray, target: Int): Int {",
        "    var low = 0",
        "    var high = items.lastIndex",
        "    while (low <= high) {",
        "        val mid = low + (high - low) / 2",
        "        when {",
        "            items[mid] == target -> return mid",
        "            items[mid] < target -> low = mid + 1",
        "            else -> high = mid - 1",
        "        }",
        "    }",
        "    return -1",
        "}"
      ].join("\n")
    },
    rust: {
      file: "binary_search.rs",
      code: [
        "fn binary_search(items: &[i32], target: i32) -> Option<usize> {",
        "    let (mut low, mut high) = (0, items.len());",
        "    while low < high {",
        "        let mid = low + (high - low) / 2;",
        "        match items[mid].cmp(&target) {",
        "            std::cmp::Ordering::Equal => return Some(mid),",
        "            std::cmp::Ordering::Less => low = mid + 1,",
        "            std::cmp::Ordering::Greater => high = mid,",
        "        }",
        "    }",
        "    None",
        "}"
      ].join("\n")
    }
  };

  const codeElement = document.querySelector("#code-sample code");
  const fileElement = document.getElementById("sample-file");
  const tabs = document.querySelectorAll("[data-code-tab]");
  let activeSnippet = "python";

  const selectSnippet = (language) => {
    const snippet = snippets[language];
    if (!snippet || !codeElement || !fileElement) return;
    activeSnippet = language;
    codeElement.textContent = snippet.code;
    fileElement.textContent = snippet.file;
    tabs.forEach((tab) => {
      const active = tab.dataset.codeTab === language;
      tab.classList.toggle("is-active", active);
      tab.setAttribute("aria-pressed", String(active));
    });
    const copyButton = document.getElementById("copy-sample");
    if (copyButton) copyButton.textContent = "Copy example";
  };

  tabs.forEach((tab) => {
    tab.addEventListener("click", () => selectSnippet(tab.dataset.codeTab));
  });

  const copyButton = document.getElementById("copy-sample");
  if (copyButton && codeElement) {
    copyButton.addEventListener("click", async () => {
      const text = snippets[activeSnippet].code;
      try {
        await navigator.clipboard.writeText(text);
        copyButton.textContent = "Copied";
      } catch {
        copyButton.textContent = "Copy unavailable";
      }
      window.setTimeout(() => {
        copyButton.textContent = "Copy example";
      }, 1600);
    });
  }

  const year = document.getElementById("copyright-year");
  if (year) year.textContent = String(new Date().getFullYear());
})();