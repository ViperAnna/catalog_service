import React, {useEffect} from 'react';
import {useAuthStore} from '../store/useAuthStore';
import LoadingState from './LoadingState';

const ProtectedRoute = ({children, requiredRole}) => {
    const authEnabled = useAuthStore((s) => s.authEnabled);
    const authenticated = useAuthStore((s) => s.authenticated);
    const hasRole = useAuthStore((s) => s.hasRole);
    const login = useAuthStore((s) => s.login);

    useEffect(() => {
        if (authEnabled && !authenticated) {
            login();
        }
    }, [authEnabled, authenticated, login]);

    if (!authEnabled) return children;
    if (!authenticated) return <LoadingState message="Перенаправление на страницу входа…"/>;

    if (requiredRole && !hasRole(requiredRole)) {
        return (
            <div className="container mx-auto px-4 py-16 text-center">
                <h1 className="text-2xl font-bold text-gray-800 mb-2">Доступ запрещён</h1>
                <p className="text-gray-500">Эта страница доступна только пользователям с ролью {requiredRole}.</p>
            </div>
        );
    }

    return children;
};

export default ProtectedRoute;
