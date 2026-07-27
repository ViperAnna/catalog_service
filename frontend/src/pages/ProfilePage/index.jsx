import React, {useEffect, useState} from 'react';
import {toast} from 'react-hot-toast';
import {FiSave, FiTrash2, FiUser} from 'react-icons/fi';
import {useAuthStore} from '../../store/useAuthStore';
import LoadingState from '../../components/LoadingState';
import DeleteConfirmationModal from './components/DeleteConfirmationModal';

const emptyAddress = {
    country: '', city: '', street: '',
    numberOfHouse: '', numberOfApartment: '', postalCode: '',
};

const addressFields = [
    {name: 'country', label: 'Страна'},
    {name: 'city', label: 'Город'},
    {name: 'street', label: 'Улица'},
    {name: 'numberOfHouse', label: 'Дом'},
    {name: 'numberOfApartment', label: 'Квартира'},
    {name: 'postalCode', label: 'Индекс'},
];

const TECHNICAL_ROLES = new Set(['offline_access', 'uma_authorization']);

const displayRoles = (raw) =>
    (Array.isArray(raw) ? raw : [])
        .filter((r) => !r.startsWith('default-') && !TECHNICAL_ROLES.has(r))
        .map((r) => (r.startsWith('ROLE_') ? r.slice(5) : r));

const validate = (form) => {
    const errors = {};
    if (form.phone && !/^\+?[0-9]{7,15}$/.test(form.phone)) {
        errors.phone = 'Телефон: 7–15 цифр, можно с +';
    }
    if (form.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email)) {
        errors.email = 'Некорректный email';
    }
    return errors;
};

const ProfilePage = () => {
    const {profile, dbProfile, fetchMe, updateMe, deleteMe, logout} = useAuthStore();

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [showDelete, setShowDelete] = useState(false);
    const [errors, setErrors] = useState({});
    const [form, setForm] = useState({
        firstName: '', lastName: '', email: '',
        phone: '', nicknameTelegram: '', address: {...emptyAddress},
    });

    useEffect(() => {
        let active = true;
        (async () => {
            try {
                const data = await fetchMe();
                if (active && data) {
                    setForm({
                        firstName: data.firstName ?? '',
                        lastName: data.lastName ?? '',
                        email: data.email ?? '',
                        phone: data.phone ?? '',
                        nicknameTelegram: data.nicknameTelegram ?? '',
                        address: {...emptyAddress, ...(data.address ?? {})},
                    });
                }
            } catch {
                if (active && profile) {
                    setForm((prev) => ({
                        ...prev,
                        firstName: profile.firstName ?? '',
                        lastName: profile.lastName ?? '',
                        email: profile.email ?? '',
                    }));
                }
                toast.error('Не удалось загрузить профиль с сервера');
            } finally {
                if (active) setLoading(false);
            }
        })();
        return () => {
            active = false;
        };
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    const handleChange = (e) => {
        const {name, value} = e.target;
        setForm((prev) => ({...prev, [name]: value}));
    };

    const handleAddressChange = (e) => {
        const {name, value} = e.target;
        setForm((prev) => ({...prev, address: {...prev.address, [name]: value}}));
    };

    const handleSubmit = async (e) => {
        e.preventDefault();
        const errs = validate(form);
        if (Object.keys(errs).length) {
            setErrors(errs);
            return;
        }
        setErrors({});
        setSaving(true);
        try {
            const response = await updateMe(form);
            if (response?.status === 200) {
                toast.success('Профиль сохранён');
                await fetchMe();
            } else {
                toast.error('Не удалось сохранить профиль');
            }
        } catch (error) {
            toast.error(error?.message || 'Ошибка при сохранении профиля');
        } finally {
            setSaving(false);
        }
    };

    const handleDelete = async () => {
        try {
            const response = await deleteMe();
            if (response?.status === 204 || response?.status === 200) {
                toast.success('Аккаунт удалён');
                logout();
            } else {
                toast.error('Не удалось удалить аккаунт');
            }
        } catch (error) {
            toast.error(error?.message || 'Ошибка при удалении аккаунта');
        }
    };

    if (loading) return <LoadingState message="Загрузка профиля…"/>;

    const roles = displayRoles(dbProfile?.roles ?? profile?.roles);

    return (
        <div className="container mx-auto px-4 py-8 max-w-3xl">
            <div className="flex items-center gap-3 mb-8">
                <div className="w-12 h-12 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                    <FiUser className="w-6 h-6 text-white"/>
                </div>
                <div>
                    <h1 className="text-2xl font-bold text-gray-800">Мой профиль</h1>
                    <p className="text-sm text-gray-500">
                        {profile?.username ? `@${profile.username}` : 'Личные данные'}
                    </p>
                </div>
            </div>

            {roles.length > 0 && (
                <div className="flex flex-wrap items-center gap-2 mb-6">
                    <span className="text-sm text-gray-500">Роли:</span>
                    {roles.map((r) => (
                        <span key={r} className="px-2.5 py-1 text-xs font-medium bg-emerald-50 text-emerald-700 rounded-full">
                            {r}
                        </span>
                    ))}
                </div>
            )}

            <form onSubmit={handleSubmit} className="bg-white rounded-2xl shadow-sm border border-gray-100 p-6 space-y-6">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                    <Field label="Имя" name="firstName" value={form.firstName} onChange={handleChange} maxLength={20}/>
                    <Field label="Фамилия" name="lastName" value={form.lastName} onChange={handleChange} maxLength={20}/>
                    <Field label="Email" name="email" type="email" value={form.email} onChange={handleChange} maxLength={100} error={errors.email}/>
                    <Field label="Телефон" name="phone" value={form.phone} onChange={handleChange} placeholder="+79991234567" error={errors.phone}/>
                    <Field label="Telegram" name="nicknameTelegram" value={form.nicknameTelegram} onChange={handleChange} maxLength={20}/>
                </div>

                <div>
                    <h2 className="text-sm font-semibold text-gray-700 mb-3 uppercase tracking-wide">Адрес</h2>
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        {addressFields.map((f) => (
                            <Field
                                key={f.name}
                                label={f.label}
                                name={f.name}
                                value={form.address[f.name]}
                                onChange={handleAddressChange}
                                maxLength={20}
                            />
                        ))}
                    </div>
                </div>

                <div className="flex flex-col sm:flex-row justify-between gap-4 pt-4 border-t border-gray-100">
                    <button
                        type="button"
                        onClick={() => setShowDelete(true)}
                        className="flex items-center justify-center gap-2 px-5 py-3 font-medium text-red-600 hover:bg-red-50 rounded-lg transition-colors">
                        <FiTrash2 className="w-5 h-5"/>
                        Удалить аккаунт
                    </button>
                    <button
                        type="submit"
                        disabled={saving}
                        className="flex items-center justify-center gap-2 px-6 py-3 font-medium rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 disabled:cursor-not-allowed transition-all">
                        {saving ? (
                            <>
                                <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                                Сохранение…
                            </>
                        ) : (
                            <>
                                <FiSave className="w-5 h-5"/>
                                Сохранить
                            </>
                        )}
                    </button>
                </div>
            </form>

            {showDelete && (
                <DeleteConfirmationModal
                    onConfirm={handleDelete}
                    onCancel={() => setShowDelete(false)}
                />
            )}
        </div>
    );
};

const Field = ({label, name, value, onChange, type = 'text', error, ...rest}) => (
    <div>
        <label className="block text-sm font-medium text-gray-700 mb-2">{label}</label>
        <input
            type={type}
            name={name}
            value={value}
            onChange={onChange}
            className={`w-full px-4 py-3 border rounded-lg focus:ring-2 outline-none transition-all ${
                error ? 'border-red-400 focus:ring-red-400' : 'border-gray-300 focus:ring-emerald-500 focus:border-emerald-500'
            }`}
            {...rest}
        />
        {error && <p className="mt-1 text-sm text-red-600">{error}</p>}
    </div>
);

export default ProfilePage;
