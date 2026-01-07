import React from 'react';

const Header = () => {
    return (
        <nav className="bg-white shadow-lg border-b border-green-100">
            <div className="container mx-auto px-4 py-4">
                <div className="flex justify-between items-center">
                    <div className="flex items-center space-x-3">
                        <div
                            className="w-10 h-10 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                            <span className="text-white font-bold text-lg">MM</span>
                        </div>
                        <div>
                            <h1 className="text-2xl font-bold text-gray-800">MiniMarket</h1>
                            <p className="text-sm text-emerald-600">Экологичные покупки</p>
                        </div>
                    </div>
                    <button
                        className="px-6 py-2 bg-gradient-to-r from-green-500 to-emerald-600 text-white rounded-lg hover:from-green-600 hover:to-emerald-700 transition-all duration-300 shadow-md hover:shadow-lg">
                        Войти
                    </button>
                </div>
            </div>
        </nav>
    );
};

export default Header;