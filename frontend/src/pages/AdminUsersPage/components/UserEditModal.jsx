import React, {useState} from 'react';
import {FiX, FiUser} from 'react-icons/fi';

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

const UserEditModal = ({user, onSubmit, onCancel}) => {
    const [form, setForm] = useState({
        firstName: user.firstName ?? '',
        lastName: user.lastName ?? '',
        email: user.email ?? '',
        phone: user.phone ?? '',
        nicknameTelegram: user.nicknameTelegram ?? '',
        address: {...emptyAddress, ...(user.address ?? {})},
    });
    const [errors, setErrors] = useState({});
    const [busy, setBusy] = useState(false);

    const change = (e) => setForm((p) => ({...p, [e.target.name]: e.target.value}));
    const changeAddress = (e) => setForm((p) => ({...p, address: {...p.address, [e.target.name]: e.target.value}}));

    const handleSubmit = async (e) => {
        e.preventDefault();
        const errs = validate(form);
        if (Object.keys(errs).length) {
            setErrors(errs);
            return;
        }
        setErrors({});
        setBusy(true);
        try {
            await onSubmit(form);
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4 overflow-y-auto" onClick={onCancel}>
            <div className="bg-white rounded-2xl shadow-xl max-w-2xl w-full p-6 my-8" onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center justify-between mb-5">
                    <div className="flex items-center gap-3">
                        <div className="w-11 h-11 bg-gradient-to-r from-green-500 to-emerald-600 rounded-xl flex items-center justify-center">
                            <FiUser className="w-5 h-5 text-white"/>
                        </div>
                        <h2 className="text-xl font-bold text-gray-800">Редактирование пользователя</h2>
                    </div>
                    <button onClick={onCancel} className="p-2 text-gray-400 hover:text-gray-700 rounded-lg hover:bg-gray-100 transition-colors">
                        <FiX className="w-5 h-5"/>
                    </button>
                </div>

                <form onSubmit={handleSubmit} className="space-y-6">
                    <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                        <Field label="Имя" name="firstName" value={form.firstName} onChange={change} maxLength={20}/>
                        <Field label="Фамилия" name="lastName" value={form.lastName} onChange={change} maxLength={20}/>
                        <Field label="Email" name="email" type="email" value={form.email} onChange={change} maxLength={100} error={errors.email}/>
                        <Field label="Телефон" name="phone" value={form.phone} onChange={change} placeholder="+79991234567" error={errors.phone}/>
                        <Field label="Telegram" name="nicknameTelegram" value={form.nicknameTelegram} onChange={change} maxLength={20}/>
                    </div>

                    <div>
                        <h3 className="text-sm font-semibold text-gray-700 mb-3 uppercase tracking-wide">Адрес</h3>
                        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                            {addressFields.map((f) => (
                                <Field key={f.name} label={f.label} name={f.name}
                                    value={form.address[f.name]} onChange={changeAddress} maxLength={20}/>
                            ))}
                        </div>
                    </div>

                    <div className="flex justify-end gap-3 pt-4 border-t border-gray-100">
                        <button type="button" onClick={onCancel} disabled={busy}
                            className="px-5 py-2.5 text-gray-700 font-medium hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50">
                            Отмена
                        </button>
                        <button type="submit" disabled={busy}
                            className="px-6 py-2.5 font-medium rounded-lg bg-emerald-600 text-white hover:bg-emerald-700 disabled:opacity-50 transition-colors">
                            {busy ? 'Сохранение…' : 'Сохранить'}
                        </button>
                    </div>
                </form>
            </div>
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

export default UserEditModal;
