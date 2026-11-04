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

  const year = document.getElementById("copyright-year");
  if (year) year.textContent = String(new Date().getFullYear());
})();