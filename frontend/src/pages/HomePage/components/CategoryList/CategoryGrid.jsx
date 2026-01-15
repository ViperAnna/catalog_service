import CategoryCard from './CategoryCard.jsx';

const CategoryGrid = ({categories}) => (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
        {categories.map((category) => (
            <CategoryCard key={category.id} category={category} compact={false}/>
        ))}
    </div>
);

export default CategoryGrid;