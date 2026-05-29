import React from 'react';
import {Link, useLocation} from 'react-router-dom';
import {FiGrid, FiPackage} from 'react-icons/fi';

const Header = () => {
    const location = useLocation();

    const navLinks = [
        {to: '/', label: 'Категории', icon: FiGrid},
        {to: '/products', label: 'Товары', icon: FiPackage},
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

                    <button
                        className="px-6 py-2 bg-gradient-to-r from-green-500 to-emerald-600 text-white rounded-lg hover:from-green-600 hover:to-emerald-700 transition-all duration-300 shadow-md hover:shadow-lg">
                        Войти
                    </button>
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
