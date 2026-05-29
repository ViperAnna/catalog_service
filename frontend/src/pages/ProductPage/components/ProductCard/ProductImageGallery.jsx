import React, {useState} from 'react';
import {FiChevronLeft, FiChevronRight} from 'react-icons/fi';
import EmptyImage from '../../../../components/EmptyImage.jsx';

const ProductImageGallery = ({images, productName}) => {
    const [activeIdx, setActiveIdx] = useState(0);

    return (
        <div className="md:w-2/5 bg-white p-6 md:p-8 border-r border-gray-100">
            {!images || images.length === 0 ? (
                <div className="h-[300px] md:h-80 flex items-center justify-center bg-gray-50 rounded-xl">
                    <EmptyImage size="xl"/>
                </div>
            ) : (
                <div>
                    <div className="relative group h-[300px] md:h-80 bg-gray-50 rounded-xl overflow-hidden">
                        <img
                            src={images[activeIdx]}
                            alt={`${productName} — фото ${activeIdx + 1}`}
                            className="w-full h-full object-contain p-4"
                        />

                        {images.length > 1 && (
                            <>
                                <button
                                    onClick={() => setActiveIdx(i => (i - 1 + images.length) % images.length)}
                                    className="absolute left-2 top-1/2 -translate-y-1/2 p-2 bg-white/80 backdrop-blur-sm rounded-full shadow hover:bg-white transition-all opacity-0 group-hover:opacity-100"
                                >
                                    <FiChevronLeft className="w-5 h-5"/>
                                </button>
                                <button
                                    onClick={() => setActiveIdx(i => (i + 1) % images.length)}
                                    className="absolute right-2 top-1/2 -translate-y-1/2 p-2 bg-white/80 backdrop-blur-sm rounded-full shadow hover:bg-white transition-all opacity-0 group-hover:opacity-100"
                                >
                                    <FiChevronRight className="w-5 h-5"/>
                                </button>
                                <div className="absolute bottom-3 left-1/2 -translate-x-1/2 flex gap-1.5">
                                    {images.map((_, i) => (
                                        <button
                                            key={i}
                                            onClick={() => setActiveIdx(i)}
                                            className={`w-2 h-2 rounded-full transition-all ${
                                                i === activeIdx ? 'bg-emerald-500 scale-125' : 'bg-gray-300'
                                            }`}
                                        />
                                    ))}
                                </div>
                            </>
                        )}
                    </div>

                    {images.length > 1 && (
                        <div className="flex gap-2 mt-3 overflow-x-auto pb-1">
                            {images.map((src, i) => (
                                <button
                                    key={i}
                                    onClick={() => setActiveIdx(i)}
                                    className={`flex-shrink-0 w-16 h-16 rounded-lg overflow-hidden border-2 transition-all ${
                                        i === activeIdx ? 'border-emerald-500' : 'border-transparent hover:border-gray-300'
                                    }`}
                                >
                                    <img src={src} alt={`thumb-${i}`} className="w-full h-full object-cover"/>
                                </button>
                            ))}
                        </div>
                    )}
                </div>
            )}
        </div>
    );
};

export default ProductImageGallery;
