import React, {useEffect, useMemo, useState} from 'react';
import {toast} from 'react-hot-toast';
import {FiUsers, FiEdit2, FiTrash2, FiMail, FiPhone, FiSearch, FiRefreshCw, FiChevronDown, FiChevronRight} from 'react-icons/fi';
import {useAdminStore, userKeycloakId} from '../../store/useAdminStore';
import LoadingState from '../../components/LoadingState';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import UserEditModal from './components/UserEditModal';

const fullName = (u) => [u.firstName, u.lastName].filter(Boolean).join(' ') || '—';

const addressLine = (a) => {
    if (!a) return '';
    return [a.country, a.city, a.street, a.numberOfHouse && `д. ${a.numberOfHouse}`,
        a.numberOfApartment && `кв. ${a.numberOfApartment}`, a.postalCode]
        .filter(Boolean).join(', ');
};

const AdminUsersPage = () => {
    const {users, loading, fetchUsers, updateUser, deleteUser} = useAdminStore();
    const [editing, setEditing] = useState(null);
    const [deleting, setDeleting] = useState(null);
    const [search, setSearch] = useState('');
    const [expanded, setExpanded] = useState(null);

    const filtered = useMemo(() => {
        const q = search.trim().toLowerCase();
        if (!q) return users;
        return users.filter((u) =>
            [u.firstName, u.lastName, u.email, u.phone, u.nicknameTelegram, addressLine(u.address)]
                .filter(Boolean)
                .some((v) => String(v).toLowerCase().includes(q))
        );
    }, [users, search]);

    const handleRefresh = async () => {
        try {
            await fetchUsers();
            toast.success('Список обновлён');
        } catch {
            toast.error('Не удалось обновить список');
        }
    };

    useEffect(() => {
        fetchUsers().catch(() => toast.error('Не удалось загрузить пользователей'));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleUpdate = async (data) => {
        const kcId = userKeycloakId(editing);
        try {
            await updateUser(kcId, data);
            toast.success('Пользователь обновлён');
            setEditing(null);
        } catch (err) {
            toast.error(err?.message || 'Не удалось обновить пользователя');
        }
    };

    const handleDelete = async () => {
        const kcId = userKeycloakId(deleting);
        try {
            await deleteUser(kcId);
            toast.success('Пользователь удалён');
            setDeleting(null);
        } catch (err) {
            toast.error(err?.message || 'Не удалось удалить пользователя');
        }
    };

    return (
        <div className="container mx-auto px-4 py-8 max-w-6xl">
            <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-8">
                <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                        <FiUsers className="w-6 h-6 text-white"/>
                    </div>
                    <div>
                        <h1 className="text-2xl font-bold text-gray-800">Пользователи</h1>
                        <p className="text-sm text-gray-500">
                            {search.trim()
                                ? `Найдено: ${filtered.length} из ${users.length}`
                                : `Всего учётных записей: ${users.length}`}
                        </p>
                    </div>
                </div>
                <button
                    onClick={handleRefresh}
                    disabled={loading}
                    className="flex items-center justify-center gap-2 px-5 py-3 font-medium rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50 transition-colors disabled:opacity-50">
                    <FiRefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`}/>
                    Обновить
                </button>
            </div>

            <div className="relative mb-6">
                <FiSearch className="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400"/>
                <input
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder="Поиск по имени, email, телефону, Telegram или адресу…"
                    className="w-full pl-11 pr-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-emerald-500 outline-none transition-all"
                />
            </div>

            {loading ? (
                <LoadingState message="Загрузка пользователей…"/>
            ) : filtered.length === 0 ? (
                <div className="text-center py-16 bg-white rounded-2xl border border-gray-100">
                    <FiUsers className="w-12 h-12 text-gray-300 mx-auto mb-4"/>
                    <p className="text-gray-500">
                        {search.trim() ? 'Ничего не найдено по запросу' : 'Пользователи не найдены'}
                    </p>
                </div>
            ) : (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="text-left text-gray-500 border-b border-gray-100">
                                    <th className="w-10"/>
                                    <th className="px-5 py-3 font-medium">Имя</th>
                                    <th className="px-5 py-3 font-medium">Контакты</th>
                                    <th className="px-5 py-3 font-medium">Telegram</th>
                                    <th className="px-5 py-3 font-medium text-right">Действия</th>
                                </tr>
                            </thead>
                            <tbody>
                                {filtered.map((u, idx) => {
                                    const kcId = userKeycloakId(u);
                                    const rowKey = kcId ?? u.id ?? idx;
                                    const isOpen = expanded === rowKey;
                                    return (
                                        <React.Fragment key={rowKey}>
                                        <tr className="border-b border-gray-50 last:border-0 hover:bg-gray-50/50">
                                            <td className="pl-4">
                                                <button
                                                    onClick={() => setExpanded(isOpen ? null : rowKey)}
                                                    title={isOpen ? 'Свернуть' : 'Подробнее'}
                                                    className="p-1.5 text-gray-400 hover:text-gray-700 rounded-lg transition-colors">
                                                    {isOpen ? <FiChevronDown className="w-4 h-4"/> : <FiChevronRight className="w-4 h-4"/>}
                                                </button>
                                            </td>
                                            <td className="px-5 py-4 font-medium text-gray-800">{fullName(u)}</td>
                                            <td className="px-5 py-4 text-gray-600">
                                                <div className="flex flex-col gap-1">
                                                    {u.email && (
                                                        <span className="flex items-center gap-1.5"><FiMail className="w-3.5 h-3.5 text-gray-400"/>{u.email}</span>
                                                    )}
                                                    {u.phone && (
                                                        <span className="flex items-center gap-1.5"><FiPhone className="w-3.5 h-3.5 text-gray-400"/>{u.phone}</span>
                                                    )}
                                                    {!u.email && !u.phone && '—'}
                                                </div>
                                            </td>
                                            <td className="px-5 py-4 text-gray-600">{u.nicknameTelegram ? `@${u.nicknameTelegram}` : '—'}</td>
                                            <td className="px-5 py-4">
                                                <div className="flex items-center justify-end gap-1">
                                                    <button
                                                        onClick={() => setEditing(u)}
                                                        disabled={!kcId}
                                                        title={kcId ? 'Редактировать' : 'Нет keycloakUserId в ответе бэкенда'}
                                                        className="p-2 text-gray-400 hover:text-emerald-600 hover:bg-emerald-50 rounded-lg transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                                                        <FiEdit2 className="w-4 h-4"/>
                                                    </button>
                                                    <button
                                                        onClick={() => setDeleting(u)}
                                                        disabled={!kcId}
                                                        title={kcId ? 'Удалить' : 'Нет keycloakUserId в ответе бэкенда'}
                                                        className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-lg transition-colors disabled:opacity-30 disabled:cursor-not-allowed">
                                                        <FiTrash2 className="w-4 h-4"/>
                                                    </button>
                                                </div>
                                            </td>
                                        </tr>
                                        {isOpen && (
                                            <tr className="border-b border-gray-50 last:border-0 bg-gray-50/60">
                                                <td/>
                                                <td colSpan={3} className="px-5 py-4">
                                                    <dl className="grid grid-cols-1 sm:grid-cols-2 gap-x-8 gap-y-2 text-sm">
                                                        <div className="flex gap-2">
                                                            <dt className="text-gray-500 shrink-0">Адрес:</dt>
                                                            <dd className="text-gray-800">{addressLine(u.address) || '—'}</dd>
                                                        </div>
                                                        <div className="flex gap-2 min-w-0">
                                                            <dt className="text-gray-500 shrink-0">Keycloak ID:</dt>
                                                            <dd className="text-gray-800 font-mono text-xs break-all">{kcId || '—'}</dd>
                                                        </div>
                                                        <div className="flex gap-2 min-w-0">
                                                            <dt className="text-gray-500 shrink-0">ID в user-service:</dt>
                                                            <dd className="text-gray-800 font-mono text-xs break-all">{u.id || '—'}</dd>
                                                        </div>
                                                    </dl>
                                                </td>
                                                <td/>
                                            </tr>
                                        )}
                                        </React.Fragment>
                                    );
                                })}
                            </tbody>
                        </table>
                    </div>
                </div>
            )}

            {editing && (
                <UserEditModal user={editing} onSubmit={handleUpdate} onCancel={() => setEditing(null)}/>
            )}

            {deleting && (
                <ConfirmDialog
                    title="Удалить пользователя?"
                    message={`Учётная запись «${fullName(deleting)}» будет удалена из user-service.`}
                    onConfirm={handleDelete}
                    onCancel={() => setDeleting(null)}
                />
            )}
        </div>
    );
};

export default AdminUsersPage;
