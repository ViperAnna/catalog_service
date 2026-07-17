import React, {useEffect, useState} from 'react';
import {toast} from 'react-hot-toast';
import {FiUsers, FiEdit2, FiTrash2, FiMail, FiPhone} from 'react-icons/fi';
import {useAdminStore, userKeycloakId} from '../../store/useAdminStore';
import LoadingState from '../../components/LoadingState';
import ConfirmDialog from '../../components/ui/ConfirmDialog';
import UserEditModal from './components/UserEditModal';

const fullName = (u) => [u.firstName, u.lastName].filter(Boolean).join(' ') || '—';

const AdminUsersPage = () => {
    const {users, loading, fetchUsers, updateUser, deleteUser} = useAdminStore();
    const [editing, setEditing] = useState(null);
    const [deleting, setDeleting] = useState(null);

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
            <div className="flex items-center gap-3 mb-8">
                <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                    <FiUsers className="w-6 h-6 text-white"/>
                </div>
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Пользователи</h1>
                    <p className="text-sm text-gray-500">Администрирование учётных записей</p>
                </div>
            </div>

            {loading ? (
                <LoadingState message="Загрузка пользователей…"/>
            ) : users.length === 0 ? (
                <div className="text-center py-16 bg-white rounded-2xl border border-gray-100">
                    <FiUsers className="w-12 h-12 text-gray-300 mx-auto mb-4"/>
                    <p className="text-gray-500">Пользователи не найдены</p>
                </div>
            ) : (
                <div className="bg-white rounded-2xl shadow-sm border border-gray-100 overflow-hidden">
                    <div className="overflow-x-auto">
                        <table className="w-full text-sm">
                            <thead>
                                <tr className="text-left text-gray-500 border-b border-gray-100">
                                    <th className="px-5 py-3 font-medium">Имя</th>
                                    <th className="px-5 py-3 font-medium">Контакты</th>
                                    <th className="px-5 py-3 font-medium">Telegram</th>
                                    <th className="px-5 py-3 font-medium text-right">Действия</th>
                                </tr>
                            </thead>
                            <tbody>
                                {users.map((u, idx) => {
                                    const kcId = userKeycloakId(u);
                                    return (
                                        <tr key={kcId ?? u.id ?? idx} className="border-b border-gray-50 last:border-0 hover:bg-gray-50/50">
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
