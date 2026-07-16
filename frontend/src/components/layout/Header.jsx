import React from 'react';
import {Link, useLocation} from 'react-router-dom';
import {FiGrid, FiPackage, FiUser, FiLogOut, FiHeart, FiUsers} from 'react-icons/fi';
import {useAuthStore} from '../../store/useAuthStore';

const Header = () => {
    const location = useLocation();
    const authEnabled = useAuthStore((s) => s.authEnabled);
    const authenticated = useAuthStore((s) => s.authenticated);
    const profile = useAuthStore((s) => s.profile);
    const hasRole = useAuthStore((s) => s.hasRole);
    const login = useAuthStore((s) => s.login);
    const logout = useAuthStore((s) => s.logout);

    const displayName = profile?.firstName || profile?.username || 'Профиль';

    const showAuthedLinks = !authEnabled || authenticated;
    const showAdminLinks = !authEnabled || (authenticated && hasRole('ADMIN'));

    const navLinks = [
        {to: '/', label: 'Категории', icon: FiGrid},
        {to: '/products', label: 'Товары', icon: FiPackage},
        ...(showAuthedLinks ? [{to: '/wishlists', label: 'Списки желаний', icon: FiHeart}] : []),
        ...(showAdminLinks ? [{to: '/admin/users', label: 'Пользователи', icon: FiUsers}] : []),
    ];

    return (
        <nav className="bg-white shadow-lg border-b border-green-100">
            <div className="container mx-auto px-4 py-4">
                <div className="flex justify-between items-center">
                    <div className="flex items-center space-x-8">
                        <Link to="/" className="flex items-center space-x-3">
                            <div
                                className="w-10 h-10 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                                <span className="text-white font-bold text-lg">MM</span>
                            </div>
                            <div>
                                <h1 className="text-2xl font-bold text-gray-800">MiniMarket</h1>
                                <p className="text-sm text-emerald-600">Экологичные покупки</p>
                            </div>
                        </Link>

                        <div className="hidden md:flex items-center space-x-1">
                            {navLinks.map(({to, label, icon: Icon}) => {
                                const isActive = to === '/'
                                    ? location.pathname === '/'
                                    : location.pathname.startsWith(to);
                                return (
                                    <Link
                                        key={to}
                                        to={to}
                                        className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
                                            isActive
                                                ? 'bg-emerald-50 text-emerald-700'
                                                : 'text-gray-600 hover:bg-gray-100 hover:text-gray-900'
                                        }`}
                                    >
                                        <Icon className="w-4 h-4"/>
                                        {label}
                                    </Link>
                                );
                            })}
                        </div>
                    </div>

                    {authEnabled && authenticated ? (
                        <div className="flex items-center gap-2">
                            <Link
                                to="/profile"
                                className="flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-gray-700 hover:bg-gray-100 transition-colors">
                                <FiUser className="w-4 h-4"/>
                                <span className="hidden sm:inline max-w-[140px] truncate">{displayName}</span>
                            </Link>
                            <button
                                onClick={logout}
                                title="Выйти"
                                className="flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-gray-600 hover:bg-red-50 hover:text-red-600 transition-colors">
                                <FiLogOut className="w-4 h-4"/>
                                <span className="hidden md:inline">Выйти</span>
                            </button>
                        </div>
                    ) : (
                        <button
                            onClick={login}
                            className="px-6 py-2 bg-gradient-to-r from-green-500 to-emerald-600 text-white rounded-lg hover:from-green-600 hover:to-emerald-700 transition-all duration-300 shadow-md hover:shadow-lg">
                            Войти
                        </button>
                    )}
                </div>

                <div className="flex md:hidden items-center space-x-1 mt-3 pt-3 border-t border-gray-100">
                    {navLinks.map(({to, label, icon: Icon}) => {
                        const isActive = to === '/'
                            ? location.pathname === '/'
                            : location.pathname.startsWith(to);
                        return (
                            <Link
                                key={to}
                                to={to}
                                className={`flex items-center gap-2 px-4 py-2 rounded-lg font-medium transition-colors ${
                                    isActive
                                        ? 'bg-emerald-50 text-emerald-700'
                                        : 'text-gray-600 hover:bg-gray-100'
                                }`}
                            >
                                <Icon className="w-4 h-4"/>
                                {label}
                            </Link>
                        );
                    })}
                </div>
            </div>
        </nav>
    );
};

export default Header;
