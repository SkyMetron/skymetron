import { NavLink, Outlet } from 'react-router-dom';

const NAV_ITEMS = [
  { path: '/chat', icon: '\uD83D\uDCAC', label: 'Chat' },
  { path: '/brain', icon: '\uD83E\uDDE0', label: 'Brain View' },
  { path: '/memory', icon: '\uD83D\uDCDA', label: 'Memory' },
  { path: '/loops', icon: '\u26A1', label: 'Loops' },
  { path: '/providers', icon: '\uD83D\uDEE1\uFE0F', label: 'Providers' },
  { path: '/config', icon: '\u2699\uFE0F', label: 'Settings' },
];

export default function Layout() {
  return (
    <div className="layout">
      <aside className="sidebar">
        <div className="sidebar-header">
          <h1>SkyMetron</h1>
          <div className="subtitle">AI Operating System</div>
        </div>
        <nav className="sidebar-nav">
          {NAV_ITEMS.map(item => (
            <NavLink
              key={item.path}
              to={item.path}
              className={({ isActive }) => `nav-item${isActive ? ' active' : ''}`}
            >
              <span className="nav-icon">{item.icon}</span>
              {item.label}
            </NavLink>
          ))}
        </nav>
      </aside>
      <main className="main-area">
        <Outlet />
      </main>
    </div>
  );
}
