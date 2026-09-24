"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

export default function AppHeader() {
  const pathname = usePathname();

  return (
    <header className="app-header">
      <div className="app-header__inner">
        <Link href="/" className="app-header__brand">
          <span className="app-header__logo" aria-hidden="true">ST</span>
          Support Tickets
        </Link>
        <nav className="app-header__nav" aria-label="Main navigation">
          <Link
            href="/"
            className={`app-header__link${pathname === "/" ? " app-header__link--active" : ""}`}
          >
            Tickets
          </Link>
          <Link href="/tickets/new" className="app-header__action">
            + New Ticket
          </Link>
        </nav>
      </div>
    </header>
  );
}
