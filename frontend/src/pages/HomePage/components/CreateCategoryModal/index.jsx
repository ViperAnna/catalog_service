import {FiX} from 'react-icons/fi';
import CategoryForm from './CategoryForm.jsx';

const CreateCategoryModal = ({isOpen, onClose}) => {
    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] overflow-hidden">
                <div className="flex justify-between items-center p-6 border-b border-gray-100">
                    <div>
                        <h3 className="text-2xl font-bold text-gray-900">
                            Новая категория
                        </h3>
                        <p className="text-gray-600 mt-1">
                            Заполните информацию о категории
                        </p>
                    </div>
                    <button
                        onClick={onClose}
                        className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
                    >
                        <FiX className="w-6 h-6 text-gray-500"/>
                    </button>
                </div>
                <CategoryForm onClose={onClose}/>
            </div>
        </div>
    );
};

export default CreateCategoryModal;