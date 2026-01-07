import React from 'react';

const DeleteConfirmationModal = ({
                                     isOpen,
                                     title = "Подтверждение удаления",
                                     message = "Вы уверены, что хотите удалить этот элемент?",
                                     onConfirm,
                                     onCancel,
                                     isDeleting = false,
                                     confirmText = "Удалить",
                                     cancelText = "Отмена"
                                 }) => {
    if (!isOpen) return null;

    return (
        <>
            <div
                className="fixed inset-0 bg-white/30 backdrop-blur-md z-40"
                onClick={onCancel}
            />

            <div className="fixed inset-0 flex items-center justify-center z-50 p-4">
                <div className="bg-white rounded-lg shadow-xl max-w-md w-full border border-gray-200">
                    <div className="p-6">
                        <h3 className="text-lg font-semibold text-gray-900 mb-2">
                            {title}
                        </h3>
                        <p className="text-gray-600 mb-6">
                            {message}
                        </p>
                        <div className="flex justify-end space-x-3">
                            <button
                                type="button"
                                onClick={onCancel}
                                disabled={isDeleting}
                                className="px-4 py-2 bg-gray-100 text-gray-700 rounded-lg hover:bg-gray-200 transition-colors duration-200 font-medium disabled:opacity-50 disabled:cursor-not-allowed"
                            >
                                {cancelText}
                            </button>
                            <button
                                type="button"
                                onClick={onConfirm}
                                disabled={isDeleting}
                                className="px-4 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors duration-200 font-medium disabled:opacity-50 disabled:cursor-not-allowed flex items-center"
                            >
                                {isDeleting ? (
                                    <>
                                        <div
                                            className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2"
                                        />
                                        Удаление...
                                    </>
                                ) : (
                                    confirmText
                                )}
                            </button>
                        </div>
                    </div>
                </div>
            </div>
        </>
    );
};

export default DeleteConfirmationModal;