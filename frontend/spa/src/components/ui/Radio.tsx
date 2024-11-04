import React from 'react';

import { RadioGroup, RadioGroupItem } from '@/components/ui/shadecn/RadioGroup.tsx';

interface RadioItem {
    label: string;
    value: string;
}

interface RadioProps {
    items: RadioItem[];
    value?: string;
    onValueChange?: (value: string) => void;
    className?: string;
    layout?: 'horizontal' | 'vertical';
}

const Radio: React.FC<RadioProps> = ({ items, value, onValueChange, className, layout, ...props }) => {
    return (
        <RadioGroup value={value} onValueChange={onValueChange} className={className} {...props}>
            <div className={layout === 'horizontal' ? 'flex flex-row gap-2' : ''}>
                {items.map((item) => (
                    <label key={item.value} className="flex items-center gap-2">
                        <RadioGroupItem value={item.value} />
                        {item.label}
                    </label>
                ))}
            </div>
        </RadioGroup>
    );
};

export default Radio;
