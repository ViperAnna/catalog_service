import React, {useState} from 'react';
import {FiAlertTriangle} from 'react-icons/fi';

const ConfirmDialog = ({
    title = 'Подтвердите действие',
    message,
    confirmLabel = 'Удалить',
    cancelLabel = 'Отмена',
    onConfirm,
    onCancel,
}) => {
    const [busy, setBusy] = useState(false);

    const handleConfirm = async () => {
        setBusy(true);
        try {
            await onConfirm();
        } finally {
            setBusy(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" onClick={onCancel}>
            <div
                className="bg-white rounded-2xl shadow-xl max-w-md w-full p-6"
                onClick={(e) => e.stopPropagation()}>
                <div className="flex items-center gap-3 mb-4">
                    <div className="w-12 h-12 bg-red-100 rounded-full flex items-center justify-center">
                        <FiAlertTriangle className="w-6 h-6 text-red-600"/>
                    </div>
                    <h2 className="text-xl font-bold text-gray-800">{title}</h2>
                </div>
                <p className="text-gray-600 mb-6">{message}</p>
                <div className="flex justify-end gap-3">
                    <button
                        onClick={onCancel}
                        disabled={busy}
                        className="px-5 py-2.5 text-gray-700 font-medium hover:bg-gray-100 rounded-lg transition-colors disabled:opacity-50">
                        {cancelLabel}
                    </button>
                    <button
                        onClick={handleConfirm}
                        disabled={busy}
                        className="flex items-center gap-2 px-5 py-2.5 font-medium rounded-lg bg-red-600 text-white hover:bg-red-700 disabled:opacity-50 transition-colors">
                        {busy ? 'Выполняется…' : confirmLabel}
                    </button>
                </div>
            </div>
        </div>
    );
};

export default ConfirmDialog;
